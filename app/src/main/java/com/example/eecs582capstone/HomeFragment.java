package com.example.eecs582capstone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import com.example.eecs582capstone.eeg.BrainBitConnectionStore;

public class HomeFragment extends Fragment {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;

    private TextView sessionStatus;
    private Button btnStartSession, btnEndSession;
    private dbConnect dbHelper;
    private int currentUserId = -1;

    // Bluetooth mock state
    private TextView tvBtStatus, tvBtStatusDot;
    private Button btnBtConnect;

    // Aggregated UI elements
    private LinearLayout layoutAggregatedResults;
    private TextView tvAggVarianceText, tvAggQualityText, tvSessionCount;
    private ProgressBar pbAggVariance, pbAggQuality;

    // Optimal Focus Parameters UI
    private TextView tvOptSleep, tvOptMeal, tvOptCaffeine, tvOptMood, tvOptStress, tvOptLocation;
    private TextView tvOptGenre, tvOptLyrics, tvOptTempo;
    private ProgressBar pbOptLight, pbOptNoise, pbOptFamiliarity;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        sessionStatus = view.findViewById(R.id.sessionStatus);
        btnStartSession = view.findViewById(R.id.btnStartSession);
        btnEndSession = view.findViewById(R.id.btnEndSession);

        tvBtStatusDot = view.findViewById(R.id.tvBtStatusDot);
        tvBtStatus = view.findViewById(R.id.tvBtStatus);
        btnBtConnect = view.findViewById(R.id.btnBtConnect);

        btnBtConnect.setOnClickListener(v -> onBtConnectClicked());
        updateBluetoothStatusUI();

        // Aggregated Views
        layoutAggregatedResults = view.findViewById(R.id.layoutAggregatedResults);
        tvAggVarianceText = view.findViewById(R.id.tvAggVarianceText);
        tvAggQualityText = view.findViewById(R.id.tvAggQualityText);
        tvSessionCount = view.findViewById(R.id.tvSessionCount);
        pbAggVariance = view.findViewById(R.id.pbAggVariance);
        pbAggQuality = view.findViewById(R.id.pbAggQuality);

        // Optimal Focus Parameters Views
        tvOptSleep = view.findViewById(R.id.tvOptSleep);
        tvOptMeal = view.findViewById(R.id.tvOptMeal);
        tvOptCaffeine = view.findViewById(R.id.tvOptCaffeine);
        tvOptMood = view.findViewById(R.id.tvOptMood);
        tvOptStress = view.findViewById(R.id.tvOptStress);
        tvOptLocation = view.findViewById(R.id.tvOptLocation);
        tvOptGenre = view.findViewById(R.id.tvOptGenre);
        tvOptLyrics = view.findViewById(R.id.tvOptLyrics);
        tvOptTempo = view.findViewById(R.id.tvOptTempo);
        pbOptLight = view.findViewById(R.id.pbOptLight);
        pbOptNoise = view.findViewById(R.id.pbOptNoise);
        pbOptFamiliarity = view.findViewById(R.id.pbOptFamiliarity);

        dbHelper = new dbConnect(requireContext());

        SharedPreferences prefs = requireContext().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("email", null);

        if (userEmail != null) {
            Users user = dbHelper.getUserByEmail(userEmail);
            if (user != null) {
                currentUserId = user.getId();
            }
        }

        updateSessionUI();
        calculateAndDisplayAggregatedData();
        loadOptimalFocusParameters();

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
        if (currentUserId == -1) {
            layoutAggregatedResults.setVisibility(View.GONE);
            return;
        }

        Cursor cursor = dbHelper.getAllSavedSessions(currentUserId);

        try {
            if (cursor == null || !cursor.moveToFirst()) {
                layoutAggregatedResults.setVisibility(View.GONE);
                return;
            }

            double totalWeightedVariance = 0;
            double totalWeightedQuality = 0;
            double totalQualityWeight = 0;
            int count = 0;

            do {
                int variance = cursor.getInt(cursor.getColumnIndexOrThrow("variance_score"));
                int quality = cursor.getInt(cursor.getColumnIndexOrThrow("quality_score"));

                // Weighting: Higher quality sessions influence the focus score more
                double weight = (double) quality / 10.0;
                totalWeightedVariance += (variance * weight);
                totalWeightedQuality += quality;
                totalQualityWeight += weight;
                count++;
            } while (cursor.moveToNext());

            double aggFocus = totalQualityWeight > 0 ? totalWeightedVariance / totalQualityWeight : 0;
            double aggQuality = count > 0 ? totalWeightedQuality / count : 0;

            // UI Update
            layoutAggregatedResults.setVisibility(View.VISIBLE);
            tvAggVarianceText.setText(String.format(Locale.US, "Overall Focus Score: %.1f/10", aggFocus));
            tvAggQualityText.setText(String.format(Locale.US, "Overall Signal Quality: %.1f/10", aggQuality));
            tvSessionCount.setText("Based on " + count + " sessions");

            pbAggVariance.setProgress((int) (aggFocus * 100)); // Max 1000 for smoother bar
            pbAggQuality.setProgress((int) (aggQuality * 100));

        } finally {
            if (cursor != null) cursor.close();
        }
    }

    @SuppressLint("SetTextI18n")
    private void loadOptimalFocusParameters() {
        // Static “best data to enter” guidance for users before starting a session
        tvOptSleep.setText("Sleep: Aim for about 8 hours");
        tvOptMeal.setText("Last Meal: About 2 hours ago");
        tvOptCaffeine.setText("Caffeine: Around 100 mg max");
        tvOptMood.setText("Mood: Calm / Neutral / Focused");
        tvOptStress.setText("Stress Level: Keep near 3/10");
        tvOptLocation.setText("Location: Quiet, familiar place");
        tvOptGenre.setText("Music Genre: Instrumental / Soft background music");
        tvOptLyrics.setText("Lyrics Preference: No lyrics preferred");
        tvOptTempo.setText("Preferred Tempo (BPM): 90–110");

        pbOptLight.setProgress(5);
        pbOptNoise.setProgress(2);
        pbOptFamiliarity.setProgress(8);
    }
    private void setOptimalDefaults() {
        tvOptSleep.setText("Sleep: N/A");
        tvOptMeal.setText("Last Meal: N/A");
        tvOptCaffeine.setText("Caffeine: N/A");
        tvOptMood.setText("Mood: N/A");
        tvOptStress.setText("Stress Level: N/A");
        tvOptLocation.setText("Location: N/A");
        tvOptGenre.setText("Music Genre: N/A");
        tvOptLyrics.setText("Lyrics Preference: N/A");
        tvOptTempo.setText("Preferred Tempo (BPM): N/A");
        pbOptLight.setProgress(0);
        pbOptNoise.setProgress(0);
        pbOptFamiliarity.setProgress(0);
    }

    private String safeText(Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }

    private void showPreSessionSurvey() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_pre_session_survey, null);

        // Get all inputs from dialog
        EditText etSleep = dialogView.findViewById(R.id.etSleep);
        EditText etMeal = dialogView.findViewById(R.id.etMeal);
        EditText etCaffeine = dialogView.findViewById(R.id.etCaffeine);
        Spinner spinnerMood = dialogView.findViewById(R.id.spinnerMood);
        SeekBar sbStress = dialogView.findViewById(R.id.sbStress);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        Spinner spinnerGenre = dialogView.findViewById(R.id.spinnerGenre);
        Spinner spinnerLyrics = dialogView.findViewById(R.id.spinnerLyrics);
        SeekBar sbTempo = dialogView.findViewById(R.id.sbTempo);
        SeekBar sbLight = dialogView.findViewById(R.id.sbLight);
        SeekBar sbNoise = dialogView.findViewById(R.id.sbNoise);
        SeekBar sbFamiliarity = dialogView.findViewById(R.id.sbFamiliarity);
        EditText etSessionNotes = dialogView.findViewById(R.id.etSessionNotes);
        TextView tvNotesCharCount = dialogView.findViewById(R.id.tvNotesCharCount);

        etSessionNotes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvNotesCharCount.setText(s.length() + " / 500");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        builder.setView(dialogView)
                .setPositiveButton("Start Reading", (dialog, id) -> {

                    // Collect values
                    String sleep = etSleep.getText().toString().trim();
                    String meal = etMeal.getText().toString().trim();
                    String caffeine = etCaffeine.getText().toString().trim();
                    String mood = spinnerMood.getSelectedItem().toString();
                    int stress = sbStress.getProgress();
                    String location = etLocation.getText().toString().trim();
                    String genre = spinnerGenre.getSelectedItem().toString();
                    String lyrics = spinnerLyrics.getSelectedItem().toString();
                    int tempo = sbTempo.getProgress();
                    int light = sbLight.getProgress();
                    int noise = sbNoise.getProgress();
                    int familiarity = sbFamiliarity.getProgress();
                    String notes = etSessionNotes.getText().toString().trim();

                    // Save full session with the pre-session survey values
                    long sessionId = dbHelper.startSession(
                            currentUserId,
                            sleep,
                            meal,
                            caffeine,
                            mood,
                            stress,
                            location,
                            genre,
                            lyrics,
                            tempo,
                            light,
                            noise,
                            familiarity,
                            notes
                    );

                    if (sessionId != -1) {
                        Toast.makeText(getActivity(), "Session started!", Toast.LENGTH_SHORT).show();
                        updateSessionUI();
                    } else {
                        Toast.makeText(getActivity(), "Failed to start session", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", (dialog, id) -> dialog.cancel());

        builder.create().show();
    }

    private void updateSessionUI() {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        SharedPreferences prefs = requireActivity().getSharedPreferences("intake_quiz", Context.MODE_PRIVATE);

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

    // could probably just add this into session UI
    private void updateBluetoothStatusUI() {
        boolean connected = BrainBitConnectionStore.hasManager();

        if (connected) {
            tvBtStatus.setText("Connected");
            tvBtStatusDot.setText("●");
            tvBtStatusDot.setTextColor(android.graphics.Color.parseColor("#31802b"));
            btnBtConnect.setText("Reconnect / Change Device");
        } else {
            tvBtStatus.setText("Disconnected");
            tvBtStatusDot.setText("●");
            tvBtStatusDot.setTextColor(android.graphics.Color.parseColor("#c73d2e"));
            btnBtConnect.setText("Connect Device");
        }
    }

    private void onBtConnectClicked() {
        getParentFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, new DeviceScanFragment())
                .addToBackStack(null)
                .commit();
    }


    @Override
    public void onResume() {
        super.onResume();
        // Refresh data whenever we navigate back home
        calculateAndDisplayAggregatedData();
        loadOptimalFocusParameters();
        updateSessionUI();
        maybeRequestNotificationPermission();
        updateBluetoothStatusUI();
    }

    private void maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        // Only prompt once per install
        SharedPreferences prefs = requireContext().getSharedPreferences("app_settings", Context.MODE_PRIVATE);
        if (prefs.getBoolean("notification_permission_asked", false)) return;
        prefs.edit().putBoolean("notification_permission_asked", true).apply();

        new AlertDialog.Builder(requireContext())
                .setTitle("Enable Notifications")
                .setMessage("FocusFlow uses notifications to alert you when your EEG session results are ready to review. Tap Allow to enable notifications.")
                .setPositiveButton("Allow", (d, w) ->
                        ActivityCompat.requestPermissions(requireActivity(),
                                new String[]{Manifest.permission.POST_NOTIFICATIONS},
                                REQUEST_NOTIFICATION_PERMISSION))
                .setNegativeButton("Not Now", null)
                .show();
    }
}