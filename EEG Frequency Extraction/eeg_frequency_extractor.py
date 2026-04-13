#!/usr/bin/env python3
"""
Extract brainwave frequency measurements from a CSV EEG recording.

Expected input columns:
    t_sec, pack_num, marker, ch1, ch2, ...

What it computes for each channel:
- dominant frequency (peak in 1-45 Hz)
- relative band power for delta/theta/alpha/beta/gamma
- optional rolling dominant frequency over time windows

Example:
    python eeg_frequency_extractor.py brainbit_eeg_2026-04-03_21-39-42.csv
    python eeg_frequency_extractor.py brainbit_eeg_2026-04-03_21-39-42.csv --fs 250 --window-sec 4 --step-sec 1
"""

from __future__ import annotations

import argparse
from pathlib import Path
from typing import Dict, List, Tuple

import numpy as np
import pandas as pd
from scipy.signal import butter, filtfilt, welch


BANDS: Dict[str, Tuple[float, float]] = {
    "delta": (0.5, 4.0),
    "theta": (4.0, 8.0),
    "alpha": (8.0, 13.0),
    "beta": (13.0, 30.0),
    "gamma": (30.0, 45.0),
}


def estimate_fs(df: pd.DataFrame) -> float:
    """Estimate sample rate from t_sec if present, else pack_num."""
    if "t_sec" in df.columns:
        t = pd.to_numeric(df["t_sec"], errors="coerce").to_numpy()
        t = t[np.isfinite(t)]
        if len(t) >= 2 and (t[-1] - t[0]) > 0:
            return (len(t) - 1) / (t[-1] - t[0])
    if "pack_num" in df.columns:
        # Last-resort fallback if timestamps are bad.
        p = pd.to_numeric(df["pack_num"], errors="coerce").to_numpy()
        dp = np.diff(p)
        if len(dp) and np.all(dp == 1):
            raise ValueError(
                "Could not estimate sample rate from timestamps. Please provide --fs explicitly."
            )
    raise ValueError("Could not estimate sample rate. Please provide --fs explicitly.")



def get_channel_columns(df: pd.DataFrame) -> List[str]:
    numeric_cols = []
    for col in df.columns:
        if col.lower() in {"t_sec", "pack_num", "marker"}:
            continue
        if pd.api.types.is_numeric_dtype(df[col]):
            numeric_cols.append(col)
    if not numeric_cols:
        raise ValueError("No numeric EEG channel columns found.")
    return numeric_cols



def bandpass_filter(signal: np.ndarray, fs: float, low: float = 1.0, high: float = 45.0, order: int = 4) -> np.ndarray:
    nyq = 0.5 * fs
    low_n = max(low / nyq, 1e-6)
    high_n = min(high / nyq, 0.999999)
    if low_n >= high_n:
        raise ValueError("Invalid filter range for the given sampling rate.")
    b, a = butter(order, [low_n, high_n], btype="bandpass")
    return filtfilt(b, a, signal)



def compute_psd(signal: np.ndarray, fs: float) -> Tuple[np.ndarray, np.ndarray]:
    nperseg = int(min(len(signal), max(256, round(fs * 4))))
    if nperseg < 32:
        raise ValueError("Signal is too short for PSD estimation.")
    freqs, psd = welch(signal, fs=fs, nperseg=nperseg)
    return freqs, psd



def bandpower(freqs: np.ndarray, psd: np.ndarray, band: Tuple[float, float]) -> float:
    lo, hi = band
    mask = (freqs >= lo) & (freqs < hi)
    if mask.sum() < 2:
        return 0.0
    return float(np.trapezoid(psd[mask], freqs[mask]))



def summarize_channel(raw_signal: np.ndarray, fs: float) -> Dict[str, float]:
    signal = np.asarray(raw_signal, dtype=float)
    signal = signal[np.isfinite(signal)]
    if len(signal) < max(64, int(fs * 2)):
        raise ValueError("Channel is too short after removing NaNs.")

    signal = signal - np.nanmean(signal)
    signal = bandpass_filter(signal, fs, low=1.0, high=45.0)
    freqs, psd = compute_psd(signal, fs)

    band_limited = (freqs >= 1.0) & (freqs <= 45.0)
    dom_freq = float(freqs[band_limited][np.argmax(psd[band_limited])])

    total_power = bandpower(freqs, psd, (0.5, 45.0))
    out: Dict[str, float] = {
        "dominant_frequency_hz": dom_freq,
        "total_power_0p5_45": total_power,
    }

    for name, limits in BANDS.items():
        p = bandpower(freqs, psd, limits)
        out[f"{name}_power"] = p
        out[f"{name}_relative_power"] = (p / total_power) if total_power > 0 else np.nan

    alpha = out["alpha_power"]
    theta = out["theta_power"]
    beta = out["beta_power"]
    out["alpha_theta_ratio"] = alpha / theta if theta > 0 else np.nan
    out["beta_alpha_ratio"] = beta / alpha if alpha > 0 else np.nan
    return out



def rolling_dominant_frequency(raw_signal: np.ndarray, fs: float, window_sec: float, step_sec: float) -> pd.DataFrame:
    x = np.asarray(raw_signal, dtype=float)
    x = np.nan_to_num(x - np.nanmean(x), nan=0.0)
    x = bandpass_filter(x, fs, low=1.0, high=45.0)

    win = int(round(window_sec * fs))
    step = int(round(step_sec * fs))
    if win < 32 or step < 1:
        raise ValueError("window_sec/step_sec too small for the sample rate.")

    rows = []
    for start in range(0, len(x) - win + 1, step):
        seg = x[start : start + win]
        freqs, psd = welch(seg, fs=fs, nperseg=min(len(seg), max(128, int(fs * 2))))
        mask = (freqs >= 1.0) & (freqs <= 45.0)
        peak_freq = float(freqs[mask][np.argmax(psd[mask])])
        rows.append(
            {
                "window_start_sec": start / fs,
                "window_end_sec": (start + win) / fs,
                "dominant_frequency_hz": peak_freq,
            }
        )
    return pd.DataFrame(rows)



def main() -> None:
    parser = argparse.ArgumentParser(description="Extract brainwave frequency measurements from EEG CSV data.")
    parser.add_argument("csv_path", type=Path, help="Path to EEG CSV file")
    parser.add_argument("--fs", type=float, default=None, help="Sampling rate in Hz (default: estimate from t_sec)")
    parser.add_argument("--window-sec", type=float, default=4.0, help="Window length for rolling dominant frequency")
    parser.add_argument("--step-sec", type=float, default=1.0, help="Step size for rolling dominant frequency")
    parser.add_argument("--no-rolling", action="store_true", help="Skip rolling dominant-frequency output")
    args = parser.parse_args()

    df = pd.read_csv(args.csv_path)
    fs = args.fs if args.fs is not None else estimate_fs(df)
    channels = get_channel_columns(df)

    summary_rows = []
    out_dir = args.csv_path.parent

    for ch in channels:
        metrics = summarize_channel(df[ch].to_numpy(), fs)
        metrics["channel"] = ch
        summary_rows.append(metrics)

        if not args.no_rolling:
            rolling = rolling_dominant_frequency(df[ch].to_numpy(), fs, args.window_sec, args.step_sec)
            rolling.to_csv(out_dir / f"{args.csv_path.stem}_{ch}_rolling_dominant_frequency.csv", index=False)

    summary = pd.DataFrame(summary_rows)
    cols = ["channel", "dominant_frequency_hz", "alpha_theta_ratio", "beta_alpha_ratio", "total_power_0p5_45"]
    cols += [f"{b}_power" for b in BANDS]
    cols += [f"{b}_relative_power" for b in BANDS]
    summary = summary[cols]
    summary.to_csv(out_dir / f"{args.csv_path.stem}_brainwave_summary.csv", index=False)

    print(f"Estimated sampling rate: {fs:.3f} Hz")
    print("\nBrainwave summary:\n")
    print(summary.to_string(index=False, float_format=lambda x: f"{x:.6g}"))
    print(f"\nSaved summary to: {out_dir / f'{args.csv_path.stem}_brainwave_summary.csv'}")
    if not args.no_rolling:
        print("Saved per-channel rolling dominant-frequency CSV files in the same folder.")


if __name__ == "__main__":
    main()
