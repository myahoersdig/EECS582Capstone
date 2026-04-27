package com.example.eecs582capstone.eeg;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-Java EEG analyzer suitable for Android integration.
 *
 * <p>Features:
 * - CSV loading for recordings in the same format as the Python analyzer
 * - Sampling-rate estimation from repeated/blocky timestamps
 * - Bandpass filtering without external scientific dependencies
 * - Welch PSD estimation via an internal FFT
 * - Dominant band classification and heuristic focus scoring
 * - Optional rolling-window analysis
 *
 * <p>CLI examples:
 * <pre>
 *   javac EegFocusAnalyzer.java
 *   java EegFocusAnalyzer --self-test
 *   java EegFocusAnalyzer EEG_session_1.csv
 * </pre>
 */
public final class EegFocusAnalyzer {
    private static final LinkedHashMap<String, double[]> BANDS = new LinkedHashMap<>();
    private static final List<String> META_COLUMNS = Arrays.asList(
            "t_sec", "pack_num", "marker", "timestamp", "time", "sample", "sample_num"
    );
    private static final double ANALYSIS_MIN_HZ = 0.5;
    private static final double ANALYSIS_MAX_HZ = 45.0;
    private static final double FOCUS_SCORE_MAX = 10.0;

    static {
        BANDS.put("delta", new double[]{0.5, 4.0});
        BANDS.put("theta", new double[]{4.0, 8.0});
        BANDS.put("alpha", new double[]{8.0, 13.0});
        BANDS.put("beta", new double[]{13.0, 30.0});
        BANDS.put("gamma", new double[]{30.0, 45.0});
    }

    private EegFocusAnalyzer() {
    }

    public static final class CsvRecording {
        public final Path sourcePath;
        public final Map<String, double[]> columns;
        public final int rowCount;

        CsvRecording(Path sourcePath, Map<String, double[]> columns, int rowCount) {
            this.sourcePath = sourcePath;
            this.columns = columns;
            this.rowCount = rowCount;
        }
    }

    public static class SegmentMetrics {
        public int sampleCount;
        public double durationSec;
        public double signalRms;
        public double dominantFrequencyHz;
        public String dominantBandByFrequency;
        public String dominantBandByPower;
        public double dominantBandPowerShare;
        public double powerMarginToRunnerUp;
        public double totalPower0p5To45;
        public double alphaThetaRatio;
        public double betaAlphaRatio;
        public double engagementIndexBetaOverAlphaPlusTheta;
        public double focusScore0To10;
        public String focusLevel;
        public final LinkedHashMap<String, Double> bandPowers = new LinkedHashMap<>();
        public final LinkedHashMap<String, Double> relativePowers = new LinkedHashMap<>();
    }

    public static final class ChannelSummary extends SegmentMetrics {
        public String channel;
    }

    public static final class WindowSummary extends SegmentMetrics {
        public double windowStartSec;
        public double windowEndSec;
    }

    public static final class RollingBandSummaryRow {
        public String channel;
        public String band;
        public int windowCount;
        public double percentWindows;
        public double meanFocusScore0To10;
        public double medianFocusScore0To10;
        public double maxFocusScore0To10;
        public double percentWindowsFocusGe6;
    }

    public static final class RecordingSummary {
        public String recordingDominantBandByPower;
        public double recordingBandPowerShare;
        public double recordingPowerMarginToRunnerUp;
        public double medianChannelDominantFrequencyHz;
        public double meanChannelDominantFrequencyHz;
        public int channelsMatchingRecordingBand;
        public int channelCount;
        public double meanChannelFocusScore0To10;
        public double medianChannelFocusScore0To10;
        public double recordingFocusScore0To10;
        public String recordingFocusLevel;
        public double meanAlphaThetaRatio;
        public double meanBetaAlphaRatio;
        public double meanEngagementIndexBetaOverAlphaPlusTheta;
        public final LinkedHashMap<String, Double> meanRelativePowers = new LinkedHashMap<>();
    }

    public static final class AnalysisResult {
        public double samplingRateHz;
        public List<ChannelSummary> channelSummaries;
        public RecordingSummary recordingSummary;
        public LinkedHashMap<String, List<WindowSummary>> rollingWindowsByChannel;
        public List<RollingBandSummaryRow> rollingBandSummaryRows;
    }

    public static CsvRecording readCsv(Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("CSV is empty: " + path);
            }

            String[] headers = splitCsvLine(headerLine);
            List<List<Double>> columnValues = new ArrayList<>();
            for (int i = 0; i < headers.length; i++) {
                columnValues.add(new ArrayList<>());
            }

            int rows = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] values = splitCsvLine(line);
                for (int i = 0; i < headers.length; i++) {
                    double parsed = Double.NaN;
                    if (i < values.length) {
                        parsed = parseDouble(values[i]);
                    }
                    columnValues.get(i).add(parsed);
                }
                rows++;
            }

            LinkedHashMap<String, double[]> columns = new LinkedHashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String key = headers[i].trim();
                double[] data = new double[columnValues.get(i).size()];
                for (int j = 0; j < data.length; j++) {
                    data[j] = columnValues.get(i).get(j);
                }
                columns.put(key, data);
            }
            return new CsvRecording(path, columns, rows);
        }
    }

    public static double estimateSamplingRate(CsvRecording recording) {
        double[] t = recording.columns.get("t_sec");
        if (t != null) {
            double[] clean = finiteOnly(t);
            if (clean.length >= 2) {
                double duration = clean[clean.length - 1] - clean[0];
                if (duration > 0.0) {
                    return (clean.length - 1) / duration;
                }

                List<Double> positiveDt = new ArrayList<>();
                for (int i = 1; i < clean.length; i++) {
                    double dt = clean[i] - clean[i - 1];
                    if (dt > 0.0) {
                        positiveDt.add(dt);
                    }
                }
                if (!positiveDt.isEmpty()) {
                    return 1.0 / median(toArray(positiveDt));
                }
            }
        }

        double[] pack = recording.columns.get("pack_num");
        if (pack != null) {
            boolean allUnitSteps = true;
            for (int i = 1; i < pack.length; i++) {
                if (!Double.isFinite(pack[i]) || !Double.isFinite(pack[i - 1])) {
                    continue;
                }
                if (Math.abs((pack[i] - pack[i - 1]) - 1.0) > 1e-9) {
                    allUnitSteps = false;
                    break;
                }
            }
            if (allUnitSteps) {
                throw new IllegalArgumentException(
                        "Could not estimate sample rate from timestamps. Provide an explicit sample rate."
                );
            }
        }

        throw new IllegalArgumentException("Could not estimate sample rate.");
    }

    public static List<String> getChannelColumns(CsvRecording recording) {
        List<String> channels = new ArrayList<>();
        for (Map.Entry<String, double[]> entry : recording.columns.entrySet()) {
            String key = entry.getKey();
            if (META_COLUMNS.contains(key.toLowerCase(Locale.US))) {
                continue;
            }
            double[] values = entry.getValue();
            int finite = 0;
            for (double value : values) {
                if (Double.isFinite(value)) {
                    finite++;
                }
            }
            if (values.length > 0 && (finite / (double) values.length) >= 0.95) {
                channels.add(key);
            }
        }
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("No EEG channel columns found.");
        }
        return channels;
    }

    public static AnalysisResult analyzeRecording(
            Map<String, double[]> channels,
            double samplingRateHz,
            double windowSec,
            double stepSec,
            boolean includeRolling
    ) {
        if (channels.isEmpty()) {
            throw new IllegalArgumentException("No channels provided.");
        }

        AnalysisResult result = new AnalysisResult();
        result.samplingRateHz = samplingRateHz;
        result.channelSummaries = new ArrayList<>();
        result.rollingWindowsByChannel = new LinkedHashMap<>();

        for (Map.Entry<String, double[]> entry : channels.entrySet()) {
            ChannelSummary summary = new ChannelSummary();
            populateSegmentMetrics(summary, entry.getValue(), samplingRateHz);
            summary.channel = entry.getKey();
            result.channelSummaries.add(summary);

            if (includeRolling) {
                List<WindowSummary> windows = rollingBandClassification(
                        entry.getValue(), samplingRateHz, windowSec, stepSec
                );
                result.rollingWindowsByChannel.put(entry.getKey(), windows);
            }
        }

        result.recordingSummary = buildRecordingSummary(result.channelSummaries);
        result.rollingBandSummaryRows = includeRolling
                ? aggregateRollingBandSummary(result.rollingWindowsByChannel)
                : Collections.emptyList();
        return result;
    }

    public static AnalysisResult analyzeCsv(
            Path csvPath,
            Double samplingRateOverrideHz,
            double windowSec,
            double stepSec,
            boolean includeRolling
    ) throws IOException {
        CsvRecording recording = readCsv(csvPath);
        double samplingRateHz = samplingRateOverrideHz != null
                ? samplingRateOverrideHz
                : estimateSamplingRate(recording);

        LinkedHashMap<String, double[]> channels = new LinkedHashMap<>();
        for (String channelName : getChannelColumns(recording)) {
            channels.put(channelName, recording.columns.get(channelName));
        }
        return analyzeRecording(channels, samplingRateHz, windowSec, stepSec, includeRolling);
    }

    public static List<WindowSummary> rollingBandClassification(
            double[] rawSignal,
            double samplingRateHz,
            double windowSec,
            double stepSec
    ) {
        double[] prepared = prepareSignal(rawSignal);
        int window = Math.max(1, (int) Math.round(windowSec * samplingRateHz));
        int step = Math.max(1, (int) Math.round(stepSec * samplingRateHz));
        if (window < Math.max(64, (int) samplingRateHz * 2)) {
            throw new IllegalArgumentException("Window is too small for analysis.");
        }

        List<WindowSummary> windows = new ArrayList<>();
        for (int start = 0; start + window <= prepared.length; start += step) {
            double[] segment = Arrays.copyOfRange(prepared, start, start + window);
            WindowSummary summary = new WindowSummary();
            populateSegmentMetrics(summary, segment, samplingRateHz);
            summary.windowStartSec = start / samplingRateHz;
            summary.windowEndSec = (start + window) / samplingRateHz;
            windows.add(summary);
        }
        return windows;
    }

    public static void writeAnalysisOutputs(
            Path csvPath,
            AnalysisResult result,
            boolean includeRolling
    ) throws IOException {
        Path outDir = csvPath.getParent() != null ? csvPath.getParent() : Paths.get(".");
        String stem = stripExtension(csvPath.getFileName().toString());

        writeChannelSummaryCsv(outDir.resolve(stem + "_brainwave_summary_java.csv"), result.channelSummaries);
        writeRecordingSummaryCsv(
                outDir.resolve(stem + "_recording_classification_java.csv"),
                result.recordingSummary
        );

        if (includeRolling) {
            writeRollingBandSummaryCsv(
                    outDir.resolve(stem + "_rolling_band_summary_java.csv"),
                    result.rollingBandSummaryRows
            );
            for (Map.Entry<String, List<WindowSummary>> entry : result.rollingWindowsByChannel.entrySet()) {
                writeRollingWindowsCsv(
                        outDir.resolve(stem + "_" + entry.getKey() + "_rolling_dominant_frequency_java.csv"),
                        entry.getValue()
                );
            }
        }
    }

    public static void runSelfTest() {
        double fs = 250.0;
        double durationSec = 20.0;
        double[] time = new double[(int) Math.round(durationSec * fs)];
        for (int i = 0; i < time.length; i++) {
            time[i] = i / fs;
        }

        LinkedHashMap<String, double[]> synthetic = new LinkedHashMap<>();
        synthetic.put("delta", synthesizeSignal(time, 2.0, 0.05, 7L));
        synthetic.put("theta", synthesizeSignal(time, 6.0, 0.05, 11L));
        synthetic.put("alpha", synthesizeSignal(time, 10.0, 0.05, 13L));
        synthetic.put("beta", synthesizeSignal(time, 20.0, 0.05, 17L));
        synthetic.put("gamma", synthesizeSignal(time, 35.0, 0.05, 19L));

        LinkedHashMap<String, Double> focusScores = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : synthetic.entrySet()) {
            ChannelSummary summary = new ChannelSummary();
            populateSegmentMetrics(summary, entry.getValue(), fs);
            if (!entry.getKey().equals(summary.dominantBandByPower)) {
                throw new IllegalStateException(
                        "Expected " + entry.getKey() + " but got " + summary.dominantBandByPower
                );
            }
            focusScores.put(entry.getKey(), summary.focusScore0To10);
        }

        int blocks = time.length / 8;
        double[] timestamps = new double[time.length];
        for (int i = 0; i < timestamps.length; i++) {
            timestamps[i] = (i / 8) * (8.0 / fs);
        }
        LinkedHashMap<String, double[]> csvColumns = new LinkedHashMap<>();
        csvColumns.put("t_sec", timestamps);
        csvColumns.put("pack_num", sequentialDoubleArray(time.length));
        csvColumns.put("marker", new double[time.length]);
        csvColumns.put("ch1", synthetic.get("alpha"));
        CsvRecording syntheticRecording = new CsvRecording(Paths.get("synthetic.csv"), csvColumns, time.length);
        double estimatedFs = estimateSamplingRate(syntheticRecording);
        if (Math.abs(estimatedFs - fs) >= 0.5) {
            throw new IllegalStateException("Sampling-rate estimate mismatch: " + estimatedFs);
        }

        List<WindowSummary> rolling = rollingBandClassification(synthetic.get("alpha"), fs, 4.0, 1.0);
        int alphaCount = 0;
        for (WindowSummary window : rolling) {
            if ("alpha".equals(window.dominantBandByPower)) {
                alphaCount++;
            }
        }
        double alphaFraction = alphaCount / (double) rolling.size();
        if (alphaFraction <= 0.95) {
            throw new IllegalStateException("Alpha rolling stability too low: " + alphaFraction);
        }

        double deltaFocus = focusScores.get("delta");
        double thetaFocus = focusScores.get("theta");
        double alphaFocus = focusScores.get("alpha");
        double betaFocus = focusScores.get("beta");
        double gammaFocus = focusScores.get("gamma");
        if (!(betaFocus > alphaFocus && alphaFocus > thetaFocus && thetaFocus > deltaFocus)) {
            throw new IllegalStateException("Unexpected focus ordering: " + focusScores);
        }
        if (!(gammaFocus < alphaFocus)) {
            throw new IllegalStateException("Gamma score should remain below alpha score.");
        }
        if (betaFocus < 8.0 || deltaFocus > 2.0) {
            throw new IllegalStateException("Focus score bounds violated: " + focusScores);
        }

        System.out.println("Self-test passed.");
        System.out.println("Validated dominant band detection for delta/theta/alpha/beta/gamma.");
        System.out.printf(Locale.US, "Validated sampling-rate estimation: %.3f Hz%n", estimatedFs);
        System.out.printf(Locale.US, "Validated rolling alpha stability: %.3f%%%n", alphaFraction * 100.0);
        System.out.println("Validated focus-score ordering: " + focusScores);
    }

    private static void populateSegmentMetrics(SegmentMetrics out, double[] rawSignal, double samplingRateHz) {
        double[] signal = prepareSignal(rawSignal);
        int minLength = Math.max(64, ((int) samplingRateHz) * 2);
        if (signal.length < minLength) {
            throw new IllegalArgumentException("Signal is too short after preprocessing.");
        }

        double mean = mean(signal);
        double[] centered = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            centered[i] = signal[i] - mean;
        }

        double[] filtered = zeroPhaseBandpass(centered, samplingRateHz, 1.0, 45.0);
        WelchResult psd = computeWelchPsd(filtered, samplingRateHz);

        int dominantIndex = findPeakIndexInRange(psd.frequencies, psd.psd, 1.0, ANALYSIS_MAX_HZ);
        double dominantFrequencyHz = psd.frequencies[dominantIndex];

        double totalPower = bandPower(psd.frequencies, psd.psd, ANALYSIS_MIN_HZ, ANALYSIS_MAX_HZ);
        LinkedHashMap<String, Double> bandPowers = new LinkedHashMap<>();
        LinkedHashMap<String, Double> relativePowers = new LinkedHashMap<>();
        for (Map.Entry<String, double[]> entry : BANDS.entrySet()) {
            double power = bandPower(psd.frequencies, psd.psd, entry.getValue()[0], entry.getValue()[1]);
            bandPowers.put(entry.getKey(), power);
            relativePowers.put(entry.getKey(), totalPower > 0.0 ? power / totalPower : 0.0);
        }

        PowerBand bandClassification = dominantPowerBand(relativePowers);
        double alphaPower = bandPowers.get("alpha");
        double thetaPower = bandPowers.get("theta");
        double betaPower = bandPowers.get("beta");
        double alphaThetaRatio = thetaPower > 0.0 ? alphaPower / thetaPower : 0.0;
        double betaAlphaRatio = alphaPower > 0.0 ? betaPower / alphaPower : 0.0;
        double engagementIndex = (alphaPower + thetaPower) > 0.0
                ? betaPower / (alphaPower + thetaPower)
                : 0.0;
        FocusScore focus = computeFocusScore(
                bandClassification.bandName,
                relativePowers,
                alphaThetaRatio,
                betaAlphaRatio,
                engagementIndex
        );

        out.sampleCount = signal.length;
        out.durationSec = signal.length / samplingRateHz;
        out.signalRms = rms(filtered);
        out.dominantFrequencyHz = dominantFrequencyHz;
        out.dominantBandByFrequency = classifyFrequencyBand(dominantFrequencyHz);
        out.dominantBandByPower = bandClassification.bandName;
        out.dominantBandPowerShare = bandClassification.share;
        out.powerMarginToRunnerUp = bandClassification.marginToRunnerUp;
        out.totalPower0p5To45 = totalPower;
        out.alphaThetaRatio = alphaThetaRatio;
        out.betaAlphaRatio = betaAlphaRatio;
        out.engagementIndexBetaOverAlphaPlusTheta = engagementIndex;
        out.focusScore0To10 = focus.score;
        out.focusLevel = focus.level;
        out.bandPowers.clear();
        out.bandPowers.putAll(bandPowers);
        out.relativePowers.clear();
        out.relativePowers.putAll(relativePowers);
    }

    private static RecordingSummary buildRecordingSummary(List<ChannelSummary> channels) {
        RecordingSummary summary = new RecordingSummary();
        LinkedHashMap<String, Double> meanRelative = new LinkedHashMap<>();
        for (String band : BANDS.keySet()) {
            double sum = 0.0;
            for (ChannelSummary channel : channels) {
                sum += channel.relativePowers.get(band);
            }
            meanRelative.put(band, sum / channels.size());
        }

        PowerBand overall = dominantPowerBand(meanRelative);
        int agreement = 0;
        for (ChannelSummary channel : channels) {
            if (overall.bandName.equals(channel.dominantBandByPower)) {
                agreement++;
            }
        }

        summary.recordingDominantBandByPower = overall.bandName;
        summary.recordingBandPowerShare = overall.share;
        summary.recordingPowerMarginToRunnerUp = overall.marginToRunnerUp;
        summary.medianChannelDominantFrequencyHz = median(extractDominantFrequencies(channels));
        summary.meanChannelDominantFrequencyHz = mean(extractDominantFrequencies(channels));
        summary.channelsMatchingRecordingBand = agreement;
        summary.channelCount = channels.size();
        summary.meanChannelFocusScore0To10 = mean(extractFocusScores(channels));
        summary.medianChannelFocusScore0To10 = median(extractFocusScores(channels));
        summary.meanAlphaThetaRatio = mean(extractAlphaThetaRatios(channels));
        summary.meanBetaAlphaRatio = mean(extractBetaAlphaRatios(channels));
        summary.meanEngagementIndexBetaOverAlphaPlusTheta = mean(extractEngagementIndices(channels));
        summary.meanRelativePowers.putAll(meanRelative);

        FocusScore focus = computeFocusScore(
                overall.bandName,
                meanRelative,
                summary.meanAlphaThetaRatio,
                summary.meanBetaAlphaRatio,
                summary.meanEngagementIndexBetaOverAlphaPlusTheta
        );
        summary.recordingFocusScore0To10 = focus.score;
        summary.recordingFocusLevel = focus.level;
        return summary;
    }

    private static List<RollingBandSummaryRow> aggregateRollingBandSummary(
            Map<String, List<WindowSummary>> rollingByChannel
    ) {
        List<RollingBandSummaryRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<WindowSummary>> entry : rollingByChannel.entrySet()) {
            String channel = entry.getKey();
            List<WindowSummary> windows = entry.getValue();
            int total = windows.size();
            double[] focusValues = new double[windows.size()];
            int focusGe6 = 0;
            for (int i = 0; i < windows.size(); i++) {
                focusValues[i] = windows.get(i).focusScore0To10;
                if (windows.get(i).focusScore0To10 >= 6.0) {
                    focusGe6++;
                }
            }

            for (String band : BANDS.keySet()) {
                int count = 0;
                for (WindowSummary window : windows) {
                    if (band.equals(window.dominantBandByPower)) {
                        count++;
                    }
                }

                RollingBandSummaryRow row = new RollingBandSummaryRow();
                row.channel = channel;
                row.band = band;
                row.windowCount = count;
                row.percentWindows = total > 0 ? 100.0 * count / total : 0.0;
                row.meanFocusScore0To10 = total > 0 ? mean(focusValues) : 0.0;
                row.medianFocusScore0To10 = total > 0 ? median(focusValues) : 0.0;
                row.maxFocusScore0To10 = total > 0 ? max(focusValues) : 0.0;
                row.percentWindowsFocusGe6 = total > 0 ? 100.0 * focusGe6 / total : 0.0;
                rows.add(row);
            }
        }
        return rows;
    }

    private static double[] prepareSignal(double[] rawSignal) {
        double[] signal = Arrays.copyOf(rawSignal, rawSignal.length);
        int finiteCount = 0;
        for (double value : signal) {
            if (Double.isFinite(value)) {
                finiteCount++;
            }
        }
        if (finiteCount < 32) {
            throw new IllegalArgumentException("Signal does not contain enough numeric samples.");
        }

        int firstFinite = -1;
        for (int i = 0; i < signal.length; i++) {
            if (Double.isFinite(signal[i])) {
                firstFinite = i;
                break;
            }
        }
        int lastFinite = -1;
        for (int i = signal.length - 1; i >= 0; i--) {
            if (Double.isFinite(signal[i])) {
                lastFinite = i;
                break;
            }
        }
        if (firstFinite < 0 || lastFinite < 0) {
            throw new IllegalArgumentException("Signal has no finite samples.");
        }

        for (int i = 0; i < firstFinite; i++) {
            signal[i] = signal[firstFinite];
        }
        for (int i = lastFinite + 1; i < signal.length; i++) {
            signal[i] = signal[lastFinite];
        }

        int index = firstFinite;
        while (index <= lastFinite) {
            if (Double.isFinite(signal[index])) {
                index++;
                continue;
            }

            int gapStart = index - 1;
            int gapEnd = index;
            while (gapEnd <= lastFinite && !Double.isFinite(signal[gapEnd])) {
                gapEnd++;
            }
            double left = signal[gapStart];
            double right = signal[gapEnd];
            int gapLength = gapEnd - gapStart;
            for (int k = 1; k < gapLength; k++) {
                double ratio = k / (double) gapLength;
                signal[gapStart + k] = left + ratio * (right - left);
            }
            index = gapEnd + 1;
        }

        return signal;
    }

    private static double[] zeroPhaseBandpass(double[] signal, double fs, double lowHz, double highHz) {
        double[] filtered = Arrays.copyOf(signal, signal.length);
        Biquad high = Biquad.highPass(fs, lowHz, 0.70710678);
        Biquad low = Biquad.lowPass(fs, highHz, 0.70710678);
        filtered = filtfilt(filtered, high);
        filtered = filtfilt(filtered, high);
        filtered = filtfilt(filtered, low);
        filtered = filtfilt(filtered, low);
        return filtered;
    }

    private static double[] filtfilt(double[] signal, Biquad filter) {
        double[] forward = filter.process(signal);
        reverseInPlace(forward);
        double[] backward = filter.reset().process(forward);
        reverseInPlace(backward);
        return backward;
    }

    private static WelchResult computeWelchPsd(double[] signal, double fs) {
        int nperseg = Math.min(signal.length, Math.max(256, (int) Math.round(fs * 4.0)));
        if (nperseg < 32) {
            throw new IllegalArgumentException("Signal too short for PSD estimation.");
        }

        int step = Math.max(1, nperseg / 2);
        int nfft = nextPowerOfTwo(nperseg);
        double[] window = hannWindow(nperseg);
        double windowEnergy = 0.0;
        for (double value : window) {
            windowEnergy += value * value;
        }

        int numFreqs = nfft / 2 + 1;
        double[] psd = new double[numFreqs];
        int segmentCount = 0;
        for (int start = 0; start + nperseg <= signal.length; start += step) {
            double[] real = new double[nfft];
            double[] imag = new double[nfft];
            for (int i = 0; i < nperseg; i++) {
                real[i] = signal[start + i] * window[i];
            }
            fft(real, imag);

            for (int k = 0; k < numFreqs; k++) {
                double power = (real[k] * real[k] + imag[k] * imag[k]) / (fs * windowEnergy);
                if (k != 0 && k != numFreqs - 1) {
                    power *= 2.0;
                }
                psd[k] += power;
            }
            segmentCount++;
        }

        if (segmentCount == 0) {
            throw new IllegalArgumentException("No Welch segments were produced.");
        }

        for (int k = 0; k < psd.length; k++) {
            psd[k] /= segmentCount;
        }

        double[] frequencies = new double[numFreqs];
        for (int k = 0; k < numFreqs; k++) {
            frequencies[k] = k * fs / nfft;
        }
        return new WelchResult(frequencies, psd);
    }

    private static double bandPower(double[] frequencies, double[] psd, double lowHz, double highHz) {
        int start = -1;
        int end = -1;
        for (int i = 0; i < frequencies.length; i++) {
            double f = frequencies[i];
            if (f >= lowHz && f < highHz) {
                if (start < 0) {
                    start = i;
                }
                end = i;
            }
        }
        if (start < 0 || end - start < 1) {
            return 0.0;
        }

        double area = 0.0;
        for (int i = start; i < end; i++) {
            double dx = frequencies[i + 1] - frequencies[i];
            area += 0.5 * (psd[i] + psd[i + 1]) * dx;
        }
        return area;
    }

    private static int findPeakIndexInRange(double[] frequencies, double[] psd, double lowHz, double highHz) {
        int bestIndex = -1;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < frequencies.length; i++) {
            double f = frequencies[i];
            if (f >= lowHz && f <= highHz && psd[i] > bestValue) {
                bestValue = psd[i];
                bestIndex = i;
            }
        }
        if (bestIndex < 0) {
            throw new IllegalStateException("No PSD samples in requested frequency range.");
        }
        return bestIndex;
    }

    private static String classifyFrequencyBand(double frequencyHz) {
        for (Map.Entry<String, double[]> entry : BANDS.entrySet()) {
            double[] band = entry.getValue();
            if (frequencyHz >= band[0] && frequencyHz < band[1]) {
                return entry.getKey();
            }
        }
        if (frequencyHz >= BANDS.get("gamma")[1]) {
            return "gamma";
        }
        return "below_delta";
    }

    private static PowerBand dominantPowerBand(Map<String, Double> relativePowers) {
        String topBand = null;
        double topShare = Double.NEGATIVE_INFINITY;
        double runnerUp = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> entry : relativePowers.entrySet()) {
            double value = entry.getValue();
            if (value > topShare) {
                runnerUp = topShare;
                topShare = value;
                topBand = entry.getKey();
            } else if (value > runnerUp) {
                runnerUp = value;
            }
        }
        if (topBand == null) {
            throw new IllegalStateException("No band powers were computed.");
        }
        if (!Double.isFinite(runnerUp)) {
            runnerUp = 0.0;
        }
        return new PowerBand(topBand, topShare, topShare - runnerUp);
    }

    private static FocusScore computeFocusScore(
            String dominantBand,
            Map<String, Double> relativePowers,
            double alphaThetaRatio,
            double betaAlphaRatio,
            double engagementIndex
    ) {
        alphaThetaRatio = Double.isFinite(alphaThetaRatio) ? alphaThetaRatio : 0.0;
        betaAlphaRatio = Double.isFinite(betaAlphaRatio) ? betaAlphaRatio : 0.0;
        engagementIndex = Double.isFinite(engagementIndex) ? engagementIndex : 0.0;

        double deltaShare = relativePowers.get("delta");
        double thetaShare = relativePowers.get("theta");
        double alphaShare = relativePowers.get("alpha");
        double betaShare = relativePowers.get("beta");
        double gammaShare = relativePowers.get("gamma");
        double fastShare = alphaShare + betaShare;

        double dominantBandBonus;
        switch (dominantBand) {
            case "beta":
                dominantBandBonus = 1.0;
                break;
            case "alpha":
                dominantBandBonus = 0.25;
                break;
            case "theta":
                dominantBandBonus = -0.5;
                break;
            case "delta":
                dominantBandBonus = -1.0;
                break;
            case "gamma":
                dominantBandBonus = -1.5;
                break;
            default:
                dominantBandBonus = 0.0;
                break;
        }

        double score = 4.0;
        score += 4.0 * clip01(fastShare / 0.35);
        score += 2.0 * clip01(betaShare / 0.15);
        score += 1.0 * clip01(alphaThetaRatio / 0.8) * clip01(alphaShare / 0.08);
        score += 0.5 * clip01(betaAlphaRatio / 2.5) * clip01(betaShare / 0.08);
        score += 1.5 * clip01(engagementIndex / 0.30) * clip01(betaShare / 0.08);
        score += dominantBandBonus;
        score -= 2.0 * clip01(deltaShare / 0.75);
        score -= 1.5 * clip01(thetaShare / 0.25);
        score -= 1.5 * clip01(gammaShare / 0.12);

        score = clamp(score, 0.0, FOCUS_SCORE_MAX);
        return new FocusScore(score, classifyFocusLevel(score));
    }

    private static String classifyFocusLevel(double score) {
        if (score >= 8.0) {
            return "very_high";
        }
        if (score >= 6.0) {
            return "high";
        }
        if (score >= 4.0) {
            return "moderate";
        }
        if (score >= 2.0) {
            return "low";
        }
        return "very_low";
    }

    private static void writeChannelSummaryCsv(Path path, List<ChannelSummary> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(String.join(",",
                    "channel",
                    "sample_count",
                    "duration_sec",
                    "signal_rms",
                    "dominant_frequency_hz",
                    "dominant_band_by_frequency",
                    "dominant_band_by_power",
                    "dominant_band_power_share",
                    "power_margin_to_runner_up",
                    "alpha_theta_ratio",
                    "beta_alpha_ratio",
                    "engagement_index_beta_over_alpha_plus_theta",
                    "focus_score_0_to_10",
                    "focus_level",
                    "total_power_0p5_45",
                    "delta_power",
                    "theta_power",
                    "alpha_power",
                    "beta_power",
                    "gamma_power",
                    "delta_relative_power",
                    "theta_relative_power",
                    "alpha_relative_power",
                    "beta_relative_power",
                    "gamma_relative_power"
            ));
            writer.newLine();

            for (ChannelSummary row : rows) {
                writer.write(String.join(",",
                        row.channel,
                        Integer.toString(row.sampleCount),
                        formatDouble(row.durationSec),
                        formatDouble(row.signalRms),
                        formatDouble(row.dominantFrequencyHz),
                        row.dominantBandByFrequency,
                        row.dominantBandByPower,
                        formatDouble(row.dominantBandPowerShare),
                        formatDouble(row.powerMarginToRunnerUp),
                        formatDouble(row.alphaThetaRatio),
                        formatDouble(row.betaAlphaRatio),
                        formatDouble(row.engagementIndexBetaOverAlphaPlusTheta),
                        formatDouble(row.focusScore0To10),
                        row.focusLevel,
                        formatDouble(row.totalPower0p5To45),
                        formatDouble(row.bandPowers.get("delta")),
                        formatDouble(row.bandPowers.get("theta")),
                        formatDouble(row.bandPowers.get("alpha")),
                        formatDouble(row.bandPowers.get("beta")),
                        formatDouble(row.bandPowers.get("gamma")),
                        formatDouble(row.relativePowers.get("delta")),
                        formatDouble(row.relativePowers.get("theta")),
                        formatDouble(row.relativePowers.get("alpha")),
                        formatDouble(row.relativePowers.get("beta")),
                        formatDouble(row.relativePowers.get("gamma"))
                ));
                writer.newLine();
            }
        }
    }

    private static void writeRecordingSummaryCsv(Path path, RecordingSummary row) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(String.join(",",
                    "recording_dominant_band_by_power",
                    "recording_band_power_share",
                    "recording_power_margin_to_runner_up",
                    "median_channel_dominant_frequency_hz",
                    "mean_channel_dominant_frequency_hz",
                    "channels_matching_recording_band",
                    "channel_count",
                    "mean_channel_focus_score_0_to_10",
                    "median_channel_focus_score_0_to_10",
                    "recording_focus_score_0_to_10",
                    "recording_focus_level",
                    "mean_alpha_theta_ratio",
                    "mean_beta_alpha_ratio",
                    "mean_engagement_index_beta_over_alpha_plus_theta",
                    "mean_delta_relative_power",
                    "mean_theta_relative_power",
                    "mean_alpha_relative_power",
                    "mean_beta_relative_power",
                    "mean_gamma_relative_power"
            ));
            writer.newLine();

            writer.write(String.join(",",
                    row.recordingDominantBandByPower,
                    formatDouble(row.recordingBandPowerShare),
                    formatDouble(row.recordingPowerMarginToRunnerUp),
                    formatDouble(row.medianChannelDominantFrequencyHz),
                    formatDouble(row.meanChannelDominantFrequencyHz),
                    Integer.toString(row.channelsMatchingRecordingBand),
                    Integer.toString(row.channelCount),
                    formatDouble(row.meanChannelFocusScore0To10),
                    formatDouble(row.medianChannelFocusScore0To10),
                    formatDouble(row.recordingFocusScore0To10),
                    row.recordingFocusLevel,
                    formatDouble(row.meanAlphaThetaRatio),
                    formatDouble(row.meanBetaAlphaRatio),
                    formatDouble(row.meanEngagementIndexBetaOverAlphaPlusTheta),
                    formatDouble(row.meanRelativePowers.get("delta")),
                    formatDouble(row.meanRelativePowers.get("theta")),
                    formatDouble(row.meanRelativePowers.get("alpha")),
                    formatDouble(row.meanRelativePowers.get("beta")),
                    formatDouble(row.meanRelativePowers.get("gamma"))
            ));
            writer.newLine();
        }
    }

    private static void writeRollingBandSummaryCsv(Path path, List<RollingBandSummaryRow> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(String.join(",",
                    "channel",
                    "band",
                    "window_count",
                    "percent_windows",
                    "mean_focus_score_0_to_10",
                    "median_focus_score_0_to_10",
                    "max_focus_score_0_to_10",
                    "percent_windows_focus_ge_6"
            ));
            writer.newLine();

            for (RollingBandSummaryRow row : rows) {
                writer.write(String.join(",",
                        row.channel,
                        row.band,
                        Integer.toString(row.windowCount),
                        formatDouble(row.percentWindows),
                        formatDouble(row.meanFocusScore0To10),
                        formatDouble(row.medianFocusScore0To10),
                        formatDouble(row.maxFocusScore0To10),
                        formatDouble(row.percentWindowsFocusGe6)
                ));
                writer.newLine();
            }
        }
    }

    private static void writeRollingWindowsCsv(Path path, List<WindowSummary> windows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path)) {
            writer.write(String.join(",",
                    "window_start_sec",
                    "window_end_sec",
                    "sample_count",
                    "duration_sec",
                    "signal_rms",
                    "dominant_frequency_hz",
                    "dominant_band_by_frequency",
                    "dominant_band_by_power",
                    "dominant_band_power_share",
                    "power_margin_to_runner_up",
                    "alpha_theta_ratio",
                    "beta_alpha_ratio",
                    "engagement_index_beta_over_alpha_plus_theta",
                    "focus_score_0_to_10",
                    "focus_level",
                    "total_power_0p5_45",
                    "delta_relative_power",
                    "theta_relative_power",
                    "alpha_relative_power",
                    "beta_relative_power",
                    "gamma_relative_power"
            ));
            writer.newLine();

            for (WindowSummary row : windows) {
                writer.write(String.join(",",
                        formatDouble(row.windowStartSec),
                        formatDouble(row.windowEndSec),
                        Integer.toString(row.sampleCount),
                        formatDouble(row.durationSec),
                        formatDouble(row.signalRms),
                        formatDouble(row.dominantFrequencyHz),
                        row.dominantBandByFrequency,
                        row.dominantBandByPower,
                        formatDouble(row.dominantBandPowerShare),
                        formatDouble(row.powerMarginToRunnerUp),
                        formatDouble(row.alphaThetaRatio),
                        formatDouble(row.betaAlphaRatio),
                        formatDouble(row.engagementIndexBetaOverAlphaPlusTheta),
                        formatDouble(row.focusScore0To10),
                        row.focusLevel,
                        formatDouble(row.totalPower0p5To45),
                        formatDouble(row.relativePowers.get("delta")),
                        formatDouble(row.relativePowers.get("theta")),
                        formatDouble(row.relativePowers.get("alpha")),
                        formatDouble(row.relativePowers.get("beta")),
                        formatDouble(row.relativePowers.get("gamma"))
                ));
                writer.newLine();
            }
        }
    }

    private static String[] splitCsvLine(String line) {
        return line.split(",", -1);
    }

    private static double parseDouble(String text) {
        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return Double.NaN;
        }
    }

    private static double[] finiteOnly(double[] values) {
        DoubleArrayBuilder builder = new DoubleArrayBuilder(values.length);
        for (double value : values) {
            if (Double.isFinite(value)) {
                builder.add(value);
            }
        }
        return builder.toArray();
    }

    private static double[] extractDominantFrequencies(List<ChannelSummary> channels) {
        double[] values = new double[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            values[i] = channels.get(i).dominantFrequencyHz;
        }
        return values;
    }

    private static double[] extractFocusScores(List<ChannelSummary> channels) {
        double[] values = new double[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            values[i] = channels.get(i).focusScore0To10;
        }
        return values;
    }

    private static double[] extractAlphaThetaRatios(List<ChannelSummary> channels) {
        double[] values = new double[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            values[i] = channels.get(i).alphaThetaRatio;
        }
        return values;
    }

    private static double[] extractBetaAlphaRatios(List<ChannelSummary> channels) {
        double[] values = new double[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            values[i] = channels.get(i).betaAlphaRatio;
        }
        return values;
    }

    private static double[] extractEngagementIndices(List<ChannelSummary> channels) {
        double[] values = new double[channels.size()];
        for (int i = 0; i < channels.size(); i++) {
            values[i] = channels.get(i).engagementIndexBetaOverAlphaPlusTheta;
        }
        return values;
    }

    private static double[] sequentialDoubleArray(int length) {
        double[] values = new double[length];
        for (int i = 0; i < length; i++) {
            values[i] = i;
        }
        return values;
    }

    private static double[] synthesizeSignal(double[] time, double frequencyHz, double noiseStd, long seed) {
        java.util.Random random = new java.util.Random(seed);
        double[] signal = new double[time.length];
        for (int i = 0; i < time.length; i++) {
            signal[i] = Math.sin(2.0 * Math.PI * frequencyHz * time[i]) + noiseStd * random.nextGaussian();
        }
        return signal;
    }

    private static double mean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return values.length == 0 ? 0.0 : sum / values.length;
    }

    private static double median(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }
        double[] copy = Arrays.copyOf(values, values.length);
        Arrays.sort(copy);
        int mid = copy.length / 2;
        if (copy.length % 2 == 0) {
            return 0.5 * (copy[mid - 1] + copy[mid]);
        }
        return copy[mid];
    }

    private static double max(double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            if (value > max) {
                max = value;
            }
        }
        return values.length == 0 ? 0.0 : max;
    }

    private static double rms(double[] values) {
        double sumSq = 0.0;
        for (double value : values) {
            sumSq += value * value;
        }
        return values.length == 0 ? 0.0 : Math.sqrt(sumSq / values.length);
    }

    private static double clamp(double value, double low, double high) {
        return Math.max(low, Math.min(value, high));
    }

    private static double clip01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static int nextPowerOfTwo(int value) {
        int n = 1;
        while (n < value) {
            n <<= 1;
        }
        return n;
    }

    private static double[] hannWindow(int length) {
        double[] window = new double[length];
        if (length == 1) {
            window[0] = 1.0;
            return window;
        }
        for (int i = 0; i < length; i++) {
            window[i] = 0.5 - 0.5 * Math.cos((2.0 * Math.PI * i) / (length - 1));
        }
        return window;
    }

    private static void fft(double[] real, double[] imag) {
        int n = real.length;
        int levels = 31 - Integer.numberOfLeadingZeros(n);
        if (1 << levels != n) {
            throw new IllegalArgumentException("FFT length must be a power of two.");
        }

        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - levels);
            if (j > i) {
                double tempReal = real[i];
                real[i] = real[j];
                real[j] = tempReal;

                double tempImag = imag[i];
                imag[i] = imag[j];
                imag[j] = tempImag;
            }
        }

        for (int size = 2; size <= n; size <<= 1) {
            int half = size >>> 1;
            double theta = -2.0 * Math.PI / size;
            double phaseStepReal = Math.cos(theta);
            double phaseStepImag = Math.sin(theta);
            for (int start = 0; start < n; start += size) {
                double currentReal = 1.0;
                double currentImag = 0.0;
                for (int j = 0; j < half; j++) {
                    int evenIndex = start + j;
                    int oddIndex = evenIndex + half;
                    double oddReal = currentReal * real[oddIndex] - currentImag * imag[oddIndex];
                    double oddImag = currentReal * imag[oddIndex] + currentImag * real[oddIndex];

                    real[oddIndex] = real[evenIndex] - oddReal;
                    imag[oddIndex] = imag[evenIndex] - oddImag;
                    real[evenIndex] += oddReal;
                    imag[evenIndex] += oddImag;

                    double nextReal = currentReal * phaseStepReal - currentImag * phaseStepImag;
                    double nextImag = currentReal * phaseStepImag + currentImag * phaseStepReal;
                    currentReal = nextReal;
                    currentImag = nextImag;
                }
            }
        }
    }

    private static void reverseInPlace(double[] values) {
        for (int i = 0, j = values.length - 1; i < j; i++, j--) {
            double tmp = values[i];
            values[i] = values[j];
            values[j] = tmp;
        }
    }

    private static double[] toArray(List<Double> values) {
        double[] out = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i);
        }
        return out;
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.10g", value);
    }

    private static final class WelchResult {
        final double[] frequencies;
        final double[] psd;

        WelchResult(double[] frequencies, double[] psd) {
            this.frequencies = frequencies;
            this.psd = psd;
        }
    }

    private static final class PowerBand {
        final String bandName;
        final double share;
        final double marginToRunnerUp;

        PowerBand(String bandName, double share, double marginToRunnerUp) {
            this.bandName = bandName;
            this.share = share;
            this.marginToRunnerUp = marginToRunnerUp;
        }
    }

    private static final class FocusScore {
        final double score;
        final String level;

        FocusScore(double score, String level) {
            this.score = score;
            this.level = level;
        }
    }

    private static final class Biquad {
        private final double b0;
        private final double b1;
        private final double b2;
        private final double a1;
        private final double a2;
        private double x1;
        private double x2;
        private double y1;
        private double y2;

        private Biquad(double b0, double b1, double b2, double a1, double a2) {
            this.b0 = b0;
            this.b1 = b1;
            this.b2 = b2;
            this.a1 = a1;
            this.a2 = a2;
        }

        static Biquad lowPass(double sampleRateHz, double cutoffHz, double q) {
            double omega = 2.0 * Math.PI * cutoffHz / sampleRateHz;
            double cos = Math.cos(omega);
            double sin = Math.sin(omega);
            double alpha = sin / (2.0 * q);

            double b0 = (1.0 - cos) * 0.5;
            double b1 = 1.0 - cos;
            double b2 = (1.0 - cos) * 0.5;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cos;
            double a2 = 1.0 - alpha;
            return new Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0);
        }

        static Biquad highPass(double sampleRateHz, double cutoffHz, double q) {
            double omega = 2.0 * Math.PI * cutoffHz / sampleRateHz;
            double cos = Math.cos(omega);
            double sin = Math.sin(omega);
            double alpha = sin / (2.0 * q);

            double b0 = (1.0 + cos) * 0.5;
            double b1 = -(1.0 + cos);
            double b2 = (1.0 + cos) * 0.5;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * cos;
            double a2 = 1.0 - alpha;
            return new Biquad(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0);
        }

        Biquad reset() {
            x1 = 0.0;
            x2 = 0.0;
            y1 = 0.0;
            y2 = 0.0;
            return this;
        }

        double[] process(double[] input) {
            reset();
            double[] output = new double[input.length];
            for (int i = 0; i < input.length; i++) {
                double x0 = input[i];
                double y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2;
                output[i] = y0;
                x2 = x1;
                x1 = x0;
                y2 = y1;
                y1 = y0;
            }
            return output;
        }
    }

    private static final class DoubleArrayBuilder {
        private double[] values;
        private int size;

        DoubleArrayBuilder(int initialCapacity) {
            this.values = new double[Math.max(8, initialCapacity)];
        }

        void add(double value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        double[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java EegFocusAnalyzer [--self-test] <csv-path> [--fs 250] [--window-sec 4] [--step-sec 1] [--no-rolling]");
            System.exit(1);
        }

        Path csvPath = null;
        Double fsOverride = null;
        double windowSec = 4.0;
        double stepSec = 1.0;
        boolean includeRolling = true;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--self-test":
                    runSelfTest();
                    return;
                case "--fs":
                    fsOverride = Double.parseDouble(args[++i]);
                    break;
                case "--window-sec":
                    windowSec = Double.parseDouble(args[++i]);
                    break;
                case "--step-sec":
                    stepSec = Double.parseDouble(args[++i]);
                    break;
                case "--no-rolling":
                    includeRolling = false;
                    break;
                default:
                    csvPath = Paths.get(args[i]);
                    break;
            }
        }

        if (csvPath == null) {
            throw new IllegalArgumentException("CSV path is required.");
        }

        AnalysisResult result = analyzeCsv(csvPath, fsOverride, windowSec, stepSec, includeRolling);
        writeAnalysisOutputs(csvPath, result, includeRolling);

        System.out.println("Input file: " + csvPath);
        System.out.printf(Locale.US, "Estimated sampling rate: %.3f Hz%n%n", result.samplingRateHz);
        System.out.println("Per-channel classification:");
        for (ChannelSummary channel : result.channelSummaries) {
            System.out.printf(
                    Locale.US,
                    "%s dom=%.3fHz band=%s share=%.4f focus=%.3f/%s%n",
                    channel.channel,
                    channel.dominantFrequencyHz,
                    channel.dominantBandByPower,
                    channel.dominantBandPowerShare,
                    channel.focusScore0To10,
                    channel.focusLevel
            );
        }

        RecordingSummary recording = result.recordingSummary;
        System.out.println();
        System.out.printf(
                Locale.US,
                "Recording focus score: %.3f/10 (%s)%n",
                recording.recordingFocusScore0To10,
                recording.recordingFocusLevel
        );
        System.out.printf(
                Locale.US,
                "Recording dominant band: %s (share=%.4f)%n",
                recording.recordingDominantBandByPower,
                recording.recordingBandPowerShare
        );
    }
}
