package com.example.eecs582capstone;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResultsFragment extends Fragment {

    private LinearLayout resultsContainer;
    private static final String PREFS_NAME = "eeg_results_prefs";
    private static final String KEY_STORED_RESULTS = "stored_sessions";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        Button btnReadData = view.findViewById(R.id.btnReadData);
        resultsContainer = view.findViewById(R.id.resultsContainer);
        
        btnReadData.setOnClickListener(v -> processEegData());

        // Load and display any previously stored results immediately
        displayStoredResults();

        return view;
    }

    /**
     * Reads from SharedPreferences and populates the UI with stored session results.
     */
    private void displayStoredResults() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedJson = prefs.getString(KEY_STORED_RESULTS, null);

        if (storedJson != null) {
            try {
                resultsContainer.removeAllViews();
                JSONArray resultsArray = new JSONArray(storedJson);
                LayoutInflater inflater = LayoutInflater.from(getContext());

                for (int i = 0; i < resultsArray.length(); i++) {
                    JSONObject result = resultsArray.getJSONObject(i);
                    addResultToUi(inflater, result);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void processEegData() {
        try {
            resultsContainer.removeAllViews();
            InputStream is = requireContext().getAssets().open("demo_sessions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject data = new JSONObject(jsonString);
            JSONArray sessions = data.getJSONArray("sessions");
            LayoutInflater inflater = LayoutInflater.from(getContext());
            
            JSONArray resultsToStore = new JSONArray();

            for (int i = 0; i < sessions.length(); i++) {
                JSONObject session = sessions.getJSONObject(i);
                String label = "Reading " + (i + 1);
                String sessionId = session.getString("sessionId");
                JSONArray samples = session.getJSONArray("samples");

                double sumQ = 0;
                int countQ = 0;
                List<Double> validV = new ArrayList<>();
                for (int j = 0; j < samples.length(); j++) {
                    JSONObject sample = samples.getJSONObject(j);
                    if (sample.has("q") && !sample.isNull("q")) {
                        sumQ += sample.getDouble("q");
                        countQ++;
                    }
                    if (sample.has("v") && !sample.isNull("v")) {
                        validV.add(sample.getDouble("v"));
                    }
                }

                double avgQ = countQ > 0 ? sumQ / countQ : 0.0;
                double completionRate = (double) validV.size() / samples.length();
                int qualityScore = (int) Math.round(avgQ * completionRate * 9) + 1;
                int varianceScore = mapVarianceTo1to10(calculateVariance(validV));

                // Create a JSON object to store the processed results
                JSONObject resultObj = new JSONObject();
                resultObj.put("label", label);
                resultObj.put("sessionId", sessionId);
                resultObj.put("varianceScore", varianceScore);
                resultObj.put("qualityScore", qualityScore);
                resultsToStore.put(resultObj);

                // Add to UI
                addResultToUi(inflater, resultObj);
            }

            // Persist the results to SharedPreferences
            SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_STORED_RESULTS, resultsToStore.toString()).apply();

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Helper to add a session result view to the container.
     */
    private void addResultToUi(LayoutInflater inflater, JSONObject result) throws JSONException {
        String label = result.getString("label");
        String sessionId = result.getString("sessionId");
        int varianceScore = result.getInt("varianceScore");
        int qualityScore = result.getInt("qualityScore");

        View sessionView = inflater.inflate(R.layout.item_session_result, resultsContainer, false);
        ((TextView) sessionView.findViewById(R.id.tvSessionLabel)).setText(label);
        ((TextView) sessionView.findViewById(R.id.tvVarianceText)).setText(String.format(Locale.US, "Variance Score: %d/10", varianceScore));
        ((ProgressBar) sessionView.findViewById(R.id.pbVariance)).setProgress(varianceScore);
        ((TextView) sessionView.findViewById(R.id.tvQualityText)).setText(String.format(Locale.US, "Quality Score: %d/10", qualityScore));
        ((ProgressBar) sessionView.findViewById(R.id.pbQuality)).setProgress(qualityScore);

        sessionView.setOnClickListener(v -> {
            SessionDetailFragment detailFragment = SessionDetailFragment.newInstance(sessionId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        resultsContainer.addView(sessionView);
    }

    private int mapVarianceTo1to10(double variance) {
        double maxVariance = 0.01; 
        double minVariance = 0.0001;
        if (variance <= minVariance) return 10;
        if (variance >= maxVariance) return 1;
        return (int) Math.round((1.0 - (variance - minVariance) / (maxVariance - minVariance)) * 9) + 1;
    }

    private double calculateVariance(List<Double> values) {
        if (values.size() < 2) return 0.0;
        double sum = 0;
        for (double v : values) sum += v;
        double mean = sum / values.size();
        double temp = 0;
        for (double v : values) temp += (v - mean) * (v - mean);
        return temp / (values.size() - 1);
    }
}
