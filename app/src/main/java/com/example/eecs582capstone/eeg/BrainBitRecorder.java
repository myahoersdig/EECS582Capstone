package com.example.eecs582capstone.eeg;

import android.content.Context;
import android.util.Log;

import com.neurosdk2.neuro.types.SignalChannelsData;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BrainBitRecorder {

    private static final String TAG = "BrainBitRecorder";

    private final ExecutorService fileExecutor = Executors.newSingleThreadExecutor();
    private PrintWriter writer;
    private long recordingStartMs;
    private volatile boolean active = false;
    private File outputFile;

    // Welford online variance — no need to buffer every sample
    private long sampleCount = 0;
    private double welfordMean = 0.0;
    private double welfordM2 = 0.0;

    // Quality: fraction of samples with meaningful amplitude
    private long totalSamples = 0;
    private long goodSamples = 0;

    // Called on main thread. Returns true if the file was opened successfully.
    public boolean start(Context context, long sessionId) {
        File dir = new File(context.getFilesDir(), "eeg_recordings");
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create recordings directory");
            return false;
        }

        String ts = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
        outputFile = new File(dir, "eeg_session_" + sessionId + "_" + ts + ".csv");

        try {
            writer = new PrintWriter(new FileWriter(outputFile));
            writer.println("t_sec,pack_num,marker,ch1,ch2,ch3,ch4");
            writer.flush();
        } catch (IOException e) {
            Log.e(TAG, "Failed to open CSV", e);
            return false;
        }

        recordingStartMs = System.currentTimeMillis();
        sampleCount = 0;
        welfordMean = 0.0;
        welfordM2 = 0.0;
        totalSamples = 0;
        goodSamples = 0;
        active = true;
        Log.d(TAG, "Recording started: " + outputFile.getAbsolutePath());
        return true;
    }

    // Called on main thread via BrainBitManager's mainHandler.
    public void onSignalData(SignalChannelsData[] data) {
        if (!active || writer == null || data == null) return;

        double tSec = (System.currentTimeMillis() - recordingStartMs) / 1000.0;

        // Build row data on main thread, then write to file in background.
        List<double[]> rows = new ArrayList<>(data.length);
        List<long[]> meta = new ArrayList<>(data.length); // [packNum, marker]

        for (SignalChannelsData packet : data) {
            double[] samples = packet.getSamples();
            if (samples == null || samples.length < 4) continue;

            rows.add(samples.clone());
            meta.add(new long[]{packet.getPackNum(), packet.getMarker()});

            // Update in-memory stats (main thread, no locking needed).
            for (int i = 0; i < 4; i++) {
                accumulate(samples[i]);
            }
        }

        if (rows.isEmpty()) return;

        final double tSecFinal = tSec;
        fileExecutor.execute(() -> {
            for (int i = 0; i < rows.size(); i++) {
                double[] s = rows.get(i);
                long[] m = meta.get(i);
                writer.printf(Locale.US, "%.6f,%d,%d,%.15f,%.15f,%.15f,%.15f%n",
                        tSecFinal, m[0], m[1], s[0], s[1], s[2], s[3]);
            }
            writer.flush();
        });
    }

    // Called on main thread.
    public void stop() {
        active = false;
        try {
            fileExecutor.submit(() -> {
                if (writer != null) {
                    writer.flush();
                    writer.close();
                    writer = null;
                }
                Log.d(TAG, "Recording stopped. samples=" + sampleCount + " file=" + outputFile);
            }).get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            Log.e(TAG, "Timed out while closing recording file", e);
            if (writer != null) {
                writer.flush();
                writer.close();
                writer = null;
            }
        }
    }

    public boolean hasData() {
        return sampleCount > 0;
    }

    public File getOutputFile() {
        return outputFile;
    }

    // variance_score 1–10: low variance = focused = high score
    public int computeVarianceScore() {
        if (sampleCount < 2) return 5;
        double variance = welfordM2 / (sampleCount - 1);

        // Log-scale mapping between empirical min/max variance thresholds.
        // Focused EEG (small drift): ~1e-12; distracted (large swings): ~1e-6.
        double minVar = 1e-12;
        double maxVar = 1e-6;
        if (variance <= minVar) return 10;
        if (variance >= maxVar) return 1;

        double ratio = (Math.log10(variance) - Math.log10(minVar))
                / (Math.log10(maxVar) - Math.log10(minVar));
        return Math.max(1, Math.min(10, (int) Math.round((1.0 - ratio) * 9) + 1));
    }

    // quality_score 1–10: fraction of samples above the "live signal" amplitude floor
    public int computeQualityScore() {
        if (totalSamples == 0) return 1;
        double ratio = (double) goodSamples / totalSamples;
        return Math.max(1, (int) Math.round(ratio * 10));
    }

    // Welford's online mean/variance accumulation.
    private void accumulate(double v) {
        totalSamples++;
        double abs = Math.abs(v);
        if (abs > 1e-7) goodSamples++; // amplitude floor matching ConnectFragment's GREEN threshold

        sampleCount++;
        double delta = v - welfordMean;
        welfordMean += delta / sampleCount;
        double delta2 = v - welfordMean;
        welfordM2 += delta * delta2;
    }
}
