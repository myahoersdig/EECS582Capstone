package com.example.eecs582capstone;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
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
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class ResultsFragment extends Fragment {

    private LinearLayout resultsContainer;

    private static class ConditionAggregate {
        String key;
        String displayText;
        int totalVariance = 0;
        int count = 0;

        ConditionAggregate(String key, String displayText) {
            this.key = key;
            this.displayText = displayText;
        }

        void addScore(int varianceScore) {
            totalVariance += varianceScore;
            count++;
        }

        double getAverageVariance() {
            return count == 0 ? 0.0 : (double) totalVariance / count;
        }
    }

    private static class RankedCondition {
        String displayText;
        double averageVariance;
        int sessionCount;

        RankedCondition(String displayText, double averageVariance, int sessionCount) {
            this.displayText = displayText;
            this.averageVariance = averageVariance;
            this.sessionCount = sessionCount;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_results, container, false);
        Button btnReadData = view.findViewById(R.id.btnReadData);
        Button btnDeleteAllData = view.findViewById(R.id.btnDeleteAllData);
        Button btnDeleteSurveyData = view.findViewById(R.id.btnDeleteSurveyData);
        resultsContainer = view.findViewById(R.id.resultsContainer);

        btnReadData.setOnClickListener(v -> processEegData());
        btnDeleteAllData.setOnClickListener(v -> confirmDeleteAllEegData());
        btnDeleteSurveyData.setOnClickListener(v -> confirmDeleteSurveyData());

        // Load and display any previously stored results immediately
        displayStoredResults();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayStoredResults();
    }

    /**
     * Reads saved EEG sessions from SQLite and populates the UI with stored session results.
     */
    private void displayStoredResults() {
        resultsContainer.removeAllViews();

        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(getContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        dbConnect dbHelper = new dbConnect(requireContext());
        Cursor cursor = dbHelper.getAllSavedSessions(userId);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        Map<String, ConditionAggregate> aggregateMap = new HashMap<>();
        List<long[]> sessionBasics = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        List<Integer> varianceScores = new ArrayList<>();
        List<Integer> qualityScores = new ArrayList<>();

        try {
            if (cursor.moveToFirst()) {
                do {
                    long dbSessionId = cursor.getLong(cursor.getColumnIndexOrThrow("session_id"));
                    String label = cursor.getString(cursor.getColumnIndexOrThrow("label"));
                    int varianceScore = cursor.getInt(cursor.getColumnIndexOrThrow("variance_score"));
                    int qualityScore = cursor.getInt(cursor.getColumnIndexOrThrow("quality_score"));

                    sessionBasics.add(new long[]{dbSessionId});
                    labels.add(label);
                    varianceScores.add(varianceScore);
                    qualityScores.add(qualityScore);

                    if (varianceScore > 0) {
                        String location = getSafeString(cursor, "location");
                        String genre = getSafeString(cursor, "music_genre");
                        String lyrics = getSafeString(cursor, "lyrics_preference");
                        int tempo = cursor.getInt(cursor.getColumnIndexOrThrow("tempo_bpm"));
                        long startTime = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));

                        String tempoBucket = getTempoBucket(tempo);
                        String timeBucket = getTimeOfDayBucket(startTime);

                        String key = location + "|" + genre + "|" + lyrics + "|" + tempoBucket + "|" + timeBucket;
                        String displayText =
                                "Location: " + location +
                                        "\nMusic: " + genre +
                                        "\nLyrics: " + lyrics +
                                        "\nTempo: " + tempoBucket +
                                        "\nTime: " + timeBucket;

                        ConditionAggregate aggregate = aggregateMap.get(key);
                        if (aggregate == null) {
                            aggregate = new ConditionAggregate(key, displayText);
                            aggregateMap.put(key, aggregate);
                        }

                        aggregate.addScore(varianceScore);
                    }

                } while (cursor.moveToNext());
            }
        } finally {
            cursor.close();
        }

        List<RankedCondition> rankedConditions = buildRankedConditions(aggregateMap);
        addRankingSectionsToUi(inflater, rankedConditions);

        for (int i = 0; i < sessionBasics.size(); i++) {
            addResultToUi(
                    inflater,
                    sessionBasics.get(i)[0],
                    labels.get(i),
                    varianceScores.get(i),
                    qualityScores.get(i)
            );
        }
    }

    private String getSafeString(Cursor cursor, String columnName) {
        int index = cursor.getColumnIndex(columnName);
        if (index == -1 || cursor.isNull(index)) {
            return "Unknown";
        }

        String value = cursor.getString(index);
        if (value == null || value.trim().isEmpty()) {
            return "Unknown";
        }

        return value.trim();
    }

    private String getTempoBucket(int tempo) {
        if (tempo <= 0) return "Unknown";
        if (tempo < 80) return "Slow";
        if (tempo <= 110) return "Moderate";
        return "Fast";
    }

    private String getTimeOfDayBucket(long startTimeMillis) {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.setTimeInMillis(startTimeMillis);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        if (hour < 12) return "Morning";
        if (hour < 17) return "Afternoon";
        if (hour < 21) return "Evening";
        return "Night";
    }

    private List<RankedCondition> buildRankedConditions(Map<String, ConditionAggregate> aggregateMap) {
        List<RankedCondition> ranked = new ArrayList<>();

        for (ConditionAggregate aggregate : aggregateMap.values()) {
            ranked.add(new RankedCondition(
                    aggregate.displayText,
                    aggregate.getAverageVariance(),
                    aggregate.count
            ));
        }

        Collections.sort(ranked, (a, b) -> Double.compare(b.averageVariance, a.averageVariance));
        return ranked;
    }

    // should make xml for this and connect it - riley
    private void addRankingSectionsToUi(LayoutInflater inflater, List<RankedCondition> rankedConditions) {
        if (rankedConditions.isEmpty()) {
            return;
        }

        View optimalHeader = inflater.inflate(android.R.layout.simple_list_item_1, resultsContainer, false);
        ((TextView) optimalHeader.findViewById(android.R.id.text1)).setText("Top 3 Most Optimal Conditions");
        resultsContainer.addView(optimalHeader);

        int topCount = Math.min(3, rankedConditions.size());
        for (int i = 0; i < topCount; i++) {
            RankedCondition ranked = rankedConditions.get(i);
            resultsContainer.addView(createRankingCard(inflater, i + 1, ranked, true));
        }

        View leastHeader = inflater.inflate(android.R.layout.simple_list_item_1, resultsContainer, false);
        ((TextView) leastHeader.findViewById(android.R.id.text1)).setText("Top 3 Least Optimal Conditions");
        resultsContainer.addView(leastHeader);

        int startLeast = Math.max(0, rankedConditions.size() - 3);
        int rankNumber = 1;
        for (int i = rankedConditions.size() - 1; i >= startLeast; i--) {
            RankedCondition ranked = rankedConditions.get(i);
            resultsContainer.addView(createRankingCard(inflater, rankNumber, ranked, false));
            rankNumber++;
        }
    }

    private View createRankingCard(LayoutInflater inflater, int rank, RankedCondition ranked, boolean optimal) {
        View card = inflater.inflate(android.R.layout.simple_list_item_2, resultsContainer, false);

        TextView title = card.findViewById(android.R.id.text1);
        TextView subtitle = card.findViewById(android.R.id.text2);

        String prefix = optimal ? "Optimal #" : "Least Optimal #";
        title.setText(prefix + rank + "  •  Avg Focus Stability: " + String.format(Locale.US, "%.1f/10", ranked.averageVariance));

        subtitle.setText(
                ranked.displayText + "\nSessions used: " + ranked.sessionCount
        );

        return card;
    }

    private void processEegData() {
        try {
            int userId = getLoggedInUserId();
            if (userId == -1) {
                Toast.makeText(getContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
                return;
            }

            dbConnect dbHelper = new dbConnect(requireContext());

            // Only process ONE completed session that has not already been turned into a reading
            long sessionId = dbHelper.getNextCompletedUnprocessedSessionId(userId);
            if (sessionId == -1) {
                Toast.makeText(getContext(), "No completed sessions are waiting to be processed.", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream is = requireContext().getAssets().open("demo_sessions.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String jsonString = new String(buffer, StandardCharsets.UTF_8);

            JSONObject data = new JSONObject(jsonString);
            JSONArray sessions = data.getJSONArray("sessions");

            // Pick one demo EEG session based on how many processed sessions already exist.
            // This keeps the demo data rotating instead of always using the same sample.
            int processedCount = dbHelper.getProcessedSessionCount(userId);
            int sessionIndex = processedCount % sessions.length();

            JSONObject session = sessions.getJSONObject(sessionIndex);
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

            String label = "Reading " + (processedCount + 1);

            // Save EEG analysis into the already existing completed session row
            boolean updated = dbHelper.saveProcessedResultsToExistingSession(
                    sessionId,
                    label,
                    varianceScore,
                    qualityScore
            );

            if (updated) {
                displayStoredResults();
                Toast.makeText(getContext(), "EEG data saved successfully.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to save EEG data.", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Helper to add a session result view to the container.
     */
    private void addResultToUi(LayoutInflater inflater, long dbSessionId, String label, int varianceScore, int qualityScore) {
        View sessionView = inflater.inflate(R.layout.item_session_result, resultsContainer, false);
        ((TextView) sessionView.findViewById(R.id.tvSessionLabel)).setText(label);
        ((TextView) sessionView.findViewById(R.id.tvVarianceText)).setText(String.format(Locale.US, "Variance Score: %d/10", varianceScore));
        ((ProgressBar) sessionView.findViewById(R.id.pbVariance)).setProgress(varianceScore);
        ((TextView) sessionView.findViewById(R.id.tvQualityText)).setText(String.format(Locale.US, "Quality Score: %d/10", qualityScore));
        ((ProgressBar) sessionView.findViewById(R.id.pbQuality)).setProgress(qualityScore);

        sessionView.setOnClickListener(v -> {
            SessionDetailFragment detailFragment = SessionDetailFragment.newInstance(dbSessionId);
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.flFragment, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        resultsContainer.addView(sessionView);
    }

    private void confirmDeleteAllEegData() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete All EEG Data")
                .setMessage("Are you sure you want to delete all saved EEG sessions?")
                .setPositiveButton("Delete", (dialog, which) -> deleteAllEegData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAllEegData() {
        int userId = getLoggedInUserId();
        if (userId == -1) {
            Toast.makeText(getContext(), "No logged in user found.", Toast.LENGTH_SHORT).show();
            return;
        }

        dbConnect dbHelper = new dbConnect(requireContext());
        dbHelper.deleteAllSessionsForUser(userId);
        resultsContainer.removeAllViews();
        Toast.makeText(getContext(), "All saved EEG data deleted.", Toast.LENGTH_SHORT).show();
    }

    private void confirmDeleteSurveyData() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Saved Survey Data")
                .setMessage("Are you sure you want to delete the saved intake survey answers?")
                .setPositiveButton("Delete", (dialog, which) -> deleteSurveyData())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSurveyData() {
        UserIntakeQuizActivity.clearSavedQuiz(requireContext());
        Toast.makeText(getContext(), "Saved survey data deleted.", Toast.LENGTH_SHORT).show();
    }

    private int getLoggedInUserId() {
        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String email = prefs.getString("email", null);

        if (email == null) {
            return -1;
        }

        dbConnect dbHelper = new dbConnect(requireContext());
        Users user = dbHelper.getUserByEmail(email);
        return user != null ? user.getId() : -1;
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