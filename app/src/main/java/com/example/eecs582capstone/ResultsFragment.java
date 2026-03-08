package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultsFragment extends Fragment {

    private TextView tvResults;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_results, container, false);

        // Link the UI elements
        Button btnReadData = view.findViewById(R.id.btnReadData);
        tvResults = view.findViewById(R.id.tvResults);

        // Set the button click listener
        btnReadData.setOnClickListener(v -> {
            processEegData();
        });

        return view;
    }

    /**
     * This method contains the "Backend Script" logic ported to Java
     * so it can run natively on the Android phone.
     */
    private void processEegData() {
        try {
            // 1. Load the "demo_sessions.json" data from the app's Assets folder
            InputStream is = requireContext().getAssets().open("demo_sessions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            // 2. Parse the JSON data
            JSONObject data = new JSONObject(jsonString);
            JSONArray sessions = data.getJSONArray("sessions");
            StringBuilder resultText = new StringBuilder("EEG Analysis Results (On-Device):\n\n");

            // 3. Loop through each session and calculate metrics
            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.getJSONObject(i);
                String label = session.getString("label");
                JSONArray samples = session.getJSONArray("samples");

                double sumQ = 0;
                int countQ = 0;
                List<Double> validV = new ArrayList<>();

                // Process individual samples
                for (int j = 0; j < samples.length(); j++) {
                    JSONObject sample = samples.getJSONObject(j);
                    
                    // Quality logic (handling nulls)
                    if (sample.has("q") && !sample.isNull("q")) {
                        sumQ += sample.getDouble("q");
                        countQ++;
                    }

                    // Variance data collection (handling nulls)
                    if (sample.has("v") && !sample.isNull("v")) {
                        validV.add(sample.getDouble("v"));
                    }
                }

                // Calculate Quality Score
                double avgQ = countQ > 0 ? sumQ / countQ : 0.0;
                double completionRate = (double) validV.size() / samples.length();
                double qualityScore = avgQ * completionRate;

                // Calculate Variance Score
                double variance = calculateVariance(validV);

                // Build the output string for the UI
                resultText.append("Session: ").append(label).append("\n")
                        .append(" - Variance Score: ").append(String.format(Locale.US, "%.6f", variance)).append("\n")
                        .append(" - Quality Score: ").append(String.format(Locale.US, "%.4f", qualityScore)).append("\n\n");
            }

            // 4. Update the Frontend TextView
            tvResults.setText(resultText.toString());

        } catch (Exception e) {
            tvResults.setText("Error reading data: " + e.getMessage());
        }
    }

    /**
     * Helper to calculate statistical variance.
     * Mirrored from Python's statistics.variance()
     */
    private double calculateVariance(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double temp = 0;
        for (double v : values) temp += (v - mean) * (v - mean);
        return temp / (values.size() - 1); // Sample variance formula
    }
}
