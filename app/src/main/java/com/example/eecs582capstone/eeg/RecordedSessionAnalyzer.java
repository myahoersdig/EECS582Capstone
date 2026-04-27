package com.example.eecs582capstone.eeg;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

public final class RecordedSessionAnalyzer {

    private static final String TAG = "RecordedSessionAnalyzer";
    private static final double QUALITY_AMPLITUDE_FLOOR = 1e-7;
    private static final double WINDOW_SEC = 4.0;
    private static final double STEP_SEC = 1.0;

    private RecordedSessionAnalyzer() {
    }

    public static final class SessionScores {
        public final int focusScore;
        public final int qualityScore;
        public final double rawFocusScore;
        public final boolean usedVarianceFallback;

        SessionScores(int focusScore, int qualityScore, double rawFocusScore, boolean usedVarianceFallback) {
            this.focusScore = focusScore;
            this.qualityScore = qualityScore;
            this.rawFocusScore = rawFocusScore;
            this.usedVarianceFallback = usedVarianceFallback;
        }
    }

    public static SessionScores analyze(File csvFile) throws IOException {
        if (csvFile == null) {
            throw new IOException("Recording file is missing.");
        }
        if (!csvFile.exists()) {
            throw new IOException("Recording file was not found: " + csvFile.getAbsolutePath());
        }

        EegFocusAnalyzer.CsvRecording recording = EegFocusAnalyzer.readCsv(csvFile.toPath());
        LinkedHashMap<String, double[]> channels = new LinkedHashMap<>();
        for (String channelName : EegFocusAnalyzer.getChannelColumns(recording)) {
            channels.put(channelName, recording.columns.get(channelName));
        }

        int qualityScore = computeQualityScore(channels);

        try {
            double samplingRateHz = EegFocusAnalyzer.estimateSamplingRate(recording);
            EegFocusAnalyzer.AnalysisResult result = EegFocusAnalyzer.analyzeRecording(
                    channels,
                    samplingRateHz,
                    WINDOW_SEC,
                    STEP_SEC,
                    false
            );
            double rawFocusScore = result.recordingSummary.recordingFocusScore0To10;
            return new SessionScores(
                    roundScore(rawFocusScore),
                    qualityScore,
                    rawFocusScore,
                    false
            );
        } catch (RuntimeException e) {
            Log.w(TAG, "Advanced focus analysis failed; using variance fallback.", e);
            int fallbackFocus = computeVarianceFallback(channels);
            return new SessionScores(fallbackFocus, qualityScore, fallbackFocus, true);
        }
    }

    private static int computeQualityScore(LinkedHashMap<String, double[]> channels) {
        long totalSamples = 0;
        long goodSamples = 0;

        for (double[] values : channels.values()) {
            for (double value : values) {
                if (!Double.isFinite(value)) {
                    continue;
                }
                totalSamples++;
                if (Math.abs(value) > QUALITY_AMPLITUDE_FLOOR) {
                    goodSamples++;
                }
            }
        }

        if (totalSamples == 0) {
            return 1;
        }

        double ratio = (double) goodSamples / totalSamples;
        return clampToScore((int) Math.round(ratio * 10.0));
    }

    private static int computeVarianceFallback(LinkedHashMap<String, double[]> channels) {
        long sampleCount = 0;
        double mean = 0.0;
        double m2 = 0.0;

        for (double[] values : channels.values()) {
            for (double value : values) {
                if (!Double.isFinite(value)) {
                    continue;
                }
                sampleCount++;
                double delta = value - mean;
                mean += delta / sampleCount;
                double delta2 = value - mean;
                m2 += delta * delta2;
            }
        }

        if (sampleCount < 2) {
            return 5;
        }

        double variance = m2 / (sampleCount - 1);
        double minVar = 1e-12;
        double maxVar = 1e-6;
        if (variance <= minVar) {
            return 10;
        }
        if (variance >= maxVar) {
            return 1;
        }

        double ratio = (Math.log10(variance) - Math.log10(minVar))
                / (Math.log10(maxVar) - Math.log10(minVar));
        return clampToScore((int) Math.round((1.0 - ratio) * 9.0) + 1);
    }

    private static int roundScore(double score) {
        return clampToScore((int) Math.round(score));
    }

    private static int clampToScore(int score) {
        return Math.max(1, Math.min(10, score));
    }
}
