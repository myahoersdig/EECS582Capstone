package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SessionDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "session_id";

    public static SessionDetailFragment newInstance(String sessionId) {
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putString(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_detail, container, false);

        // UI references
        TextView tvSleep = view.findViewById(R.id.tvSleep);
        TextView tvMeal = view.findViewById(R.id.tvMeal);
        TextView tvCaffeine = view.findViewById(R.id.tvCaffeine);
        TextView tvMood = view.findViewById(R.id.tvMood);
        TextView tvStress = view.findViewById(R.id.tvStress);
        TextView tvLocation = view.findViewById(R.id.tvLocation);
        TextView tvDateTime = view.findViewById(R.id.tvDateTime);
        ProgressBar pbLight = view.findViewById(R.id.pbLight);
        ProgressBar pbNoise = view.findViewById(R.id.pbNoise);
        ProgressBar pbFamiliarity = view.findViewById(R.id.pbFamiliarity);
        Button btnBack = view.findViewById(R.id.btnBack);

        // Fill with Mock Data
        String sessionId = getArguments() != null ? getArguments().getString(ARG_SESSION_ID) : "Unknown";

        // Logic to provide different mock data based on the session ID
        if ("demo_stable_01".equals(sessionId)) {
            setMockData(tvSleep, tvMeal, tvCaffeine, tvMood, tvStress, tvLocation, tvDateTime, pbLight, pbNoise, pbFamiliarity,
                    "8.0 hours", "2 hours", "None", "Euphoric", "2", "38.95 N, 95.23 W (Home Office)", "March 15, 9:00 AM", 8, 2, 10);
        } else if ("demo_distracted_01".equals(sessionId)) {
            setMockData(tvSleep, tvMeal, tvCaffeine, tvMood, tvStress, tvLocation, tvDateTime, pbLight, pbNoise, pbFamiliarity,
                    "5.5 hours", "6 hours", "300mg at 12:00 PM", "Anxious", "8", "38.96 N, 95.24 W (Busy Coffee Shop)", "March 15, 1:30 PM", 5, 9, 3);
        } else {
            setMockData(tvSleep, tvMeal, tvCaffeine, tvMood, tvStress, tvLocation, tvDateTime, pbLight, pbNoise, pbFamiliarity,
                    "7.0 hours", "4 hours", "80mg at 7:00 AM", "Neutral", "5", "38.95 N, 95.23 W (Library)", "March 15, 11:00 AM", 7, 4, 7);
        }

        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });

        return view;
    }

    private void setMockData(TextView sleep, TextView meal, TextView caffeine, TextView mood, TextView stress, 
                             TextView loc, TextView dt, ProgressBar light, ProgressBar noise, ProgressBar fam,
                             String sVal, String mVal, String cVal, String moVal, String stVal, String lVal, String dtVal,
                             int lProgress, int nProgress, int fProgress) {
        sleep.setText(sVal);
        meal.setText(mVal);
        caffeine.setText(cVal);
        mood.setText(moVal);
        stress.setText(stVal);
        loc.setText(lVal);
        dt.setText(dtVal);
        light.setProgress(lProgress);
        noise.setProgress(nProgress);
        fam.setProgress(fProgress);
    }
}
