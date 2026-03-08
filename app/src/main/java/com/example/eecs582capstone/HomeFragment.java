package com.example.eecs582capstone;

import android.app.AlertDialog;
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
import java.util.Locale;

public class HomeFragment extends Fragment {

    private TextView sessionStatus;
    private Button btnStartSession, btnEndSession;
    private dbConnect dbHelper;
    private int currentUserId = -1;

    // Aggregated UI elements
    private LinearLayout layoutAggregatedResults;
    private TextView tvAggVarianceText, tvAggQualityText, tvSessionCount;
    private ProgressBar pbAggVariance, pbAggQuality;

    private static final String PREFS_NAME = "eeg_results_prefs";
    private static final String KEY_STORED_RESULTS = "stored_sessions";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        sessionStatus = view.findViewById(R.id.sessionStatus);
        btnStartSession = view.findViewById(R.id.btnStartSession);
        btnEndSession = view.findViewById(R.id.btnEndSession);

        // Aggregated Views
        layoutAggregatedResults = view.findViewById(R.id.layoutAggregatedResults);
        tvAggVarianceText = view.findViewById(R.id.tvAggVarianceText);
        tvAggQualityText = view.findViewById(R.id.tvAggQualityText);
        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        pbAggVariance = view.findViewById(R.id.pbAggVariance);
        pbAggQuality = view.findViewById(R.id.pbAggQuality);

        dbHelper = new dbConnect(getActivity());

        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("email", null);

        if (userEmail != null) {
            Users user = dbHelper.getUserByEmail(userEmail);
            if (user != null) {
                currentUserId = user.getId();
            }
        }

        updateSessionUI();
        calculateAndDisplayAggregatedData();

        btnStartSession.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            showPreSessionSurvey();
        });

        btnEndSession.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            long activeId = dbHelper.getActiveSessionId(currentUserId);
            if (activeId != -1) {
                dbHelper.endSession(activeId);
                Toast.makeText(getActivity(), "Session ended", Toast.LENGTH_SHORT).show();
                updateSessionUI();
            } else {
                Toast.makeText(getActivity(), "No active session", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void calculateAndDisplayAggregatedData() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String storedJson = prefs.getString(KEY_STORED_RESULTS, null);

        if (storedJson == null) {
            layoutAggregatedResults.setVisibility(View.GONE);
            return;
        }

        try {
            JSONArray resultsArray = new JSONArray(storedJson);
            if (resultsArray.length() == 0) {
                layoutAggregatedResults.setVisibility(View.GONE);
                return;
            }

            double totalWeightedVariance = 0;
            double totalWeightedQuality = 0;
            double totalQualityWeight = 0;

            for (int i = 0; i < resultsArray.length(); i++) {
                JSONObject result = resultsArray.getJSONObject(i);
                int variance = result.getInt("varianceScore");
                int quality = result.getInt("qualityScore");

                // Weighting: Higher quality sessions influence the focus score more
                double weight = (double) quality / 10.0;
                totalWeightedVariance += (variance * weight);
                totalWeightedQuality += quality; // Simple average for quality
                totalQualityWeight += weight;
            }

            double aggFocus = totalQualityWeight > 0 ? totalWeightedVariance / totalQualityWeight : 0;
            double aggQuality = (double) totalWeightedQuality / resultsArray.length();

            // UI Update
            layoutAggregatedResults.setVisibility(View.VISIBLE);
            tvAggVarianceText.setText(String.format(Locale.US, "Overall Focus Score: %.1f/10", aggFocus));
            tvAggQualityText.setText(String.format(Locale.US, "Overall Signal Quality: %.1f/10", aggQuality));
            tvSessionCount.setText("Based on " + resultsArray.length() + " sessions");
            
            pbAggVariance.setProgress((int) (aggFocus * 100)); // Max 1000 for smoother bar
            pbAggQuality.setProgress((int) (aggQuality * 100));

        } catch (JSONException e) {
            e.printStackTrace();
            layoutAggregatedResults.setVisibility(View.GONE);
        }
    }

    private void showPreSessionSurvey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pre_session_survey, null);
        
        builder.setView(dialogView)
               .setPositiveButton("Start Reading", (dialog, id) -> startEegSession())
               .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void startEegSession() {
        long sessionId = dbHelper.startSession(currentUserId);
        if (sessionId != -1) {
            Toast.makeText(getActivity(), "Survey submitted. Session started!", Toast.LENGTH_SHORT).show();
            updateSessionUI();
        } else {
            Toast.makeText(getActivity(), "Failed to start session", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSessionUI() {
        SharedPreferences prefs = getActivity().getSharedPreferences("intake_quiz", Context.MODE_PRIVATE);

        if (currentUserId == -1) {
            sessionStatus.setText("Please log in");
            btnStartSession.setEnabled(false);
            btnEndSession.setEnabled(false);
            return;
        }

        if (!prefs.getBoolean("completed", false)) {
            sessionStatus.setText("Please complete the intake survey to unlock session recording.");
            btnStartSession.setEnabled(false);
            btnEndSession.setEnabled(false);
        } else {
            boolean active = dbHelper.hasActiveSession(currentUserId);
            if (active) {
                sessionStatus.setText("Session in progress");
                btnStartSession.setEnabled(false);
                btnEndSession.setEnabled(true);
            } else {
                sessionStatus.setText("No active session");
                btnStartSession.setEnabled(true);
                btnEndSession.setEnabled(false);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data whenever we navigate back home
        calculateAndDisplayAggregatedData();
    }
}
