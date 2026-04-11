/*
SecondFragment.java by Mya Hoersdig

initializes the second fragment (profile)
 */

package com.example.eecs582capstone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

public class ProfileFragment extends Fragment {

    private MaterialCardView quizSummaryCard;
    private TextView quizQ1, quizQ2, quizQ3, quizQ4, quizQ5, quizQ6, quizQ7, quizQ8;
    private Button intakeQuizButton;
    private TextView tvNotificationStatus;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView username = view.findViewById(R.id.profileUsername);
        TextView emailText = view.findViewById(R.id.profileEmail);

        SharedPreferences sessionPrefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);

        SharedPreferences intakePrefs = getActivity().getSharedPreferences("intake_quiz", Context.MODE_PRIVATE);


        String userEmail = sessionPrefs.getString("email", null);

        if (userEmail != null) {
            dbConnect db = new dbConnect(getActivity());
            Users user = db.getUserByEmail(userEmail);
            if (user != null) {
                username.setText(user.getFirstname() + " " + user.getLastname());
                emailText.setText(user.getEmailAddress());
            }
        }

        Button logout = view.findViewById(R.id.logoutButton);
        logout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sessionPrefs.edit();
            editor.clear();
            editor.apply();

            //clear intake quiz
            SharedPreferences.Editor editor_intake = intakePrefs.edit();
            editor_intake.clear();
            editor_intake.apply();

            Intent intent = new Intent(getActivity(), Entry.class);
            startActivity(intent);
            getActivity().finish();
        });

        intakeQuizButton = view.findViewById(R.id.intakeQuizButton);
        intakeQuizButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), UserIntakeQuizActivity.class);
            startActivity(intent);
        });

        tvNotificationStatus = view.findViewById(R.id.tvNotificationStatus);
        Button btnManageNotifications = view.findViewById(R.id.btnManageNotifications);
        btnManageNotifications.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().getPackageName());
            startActivity(intent);
        });

        quizSummaryCard = view.findViewById(R.id.quizSummaryCard);
        quizQ1 = view.findViewById(R.id.quiz_q1);
        quizQ2 = view.findViewById(R.id.quiz_q2);
        quizQ3 = view.findViewById(R.id.quiz_q3);
        quizQ4 = view.findViewById(R.id.quiz_q4);
        quizQ5 = view.findViewById(R.id.quiz_q5);
        quizQ6 = view.findViewById(R.id.quiz_q6);
        quizQ7 = view.findViewById(R.id.quiz_q7);
        quizQ8 = view.findViewById(R.id.quiz_q8);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadQuizSummary();
        updateNotificationStatus();
    }

    private void updateNotificationStatus() {
        if (tvNotificationStatus == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean granted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            tvNotificationStatus.setText(granted ? "Enabled" : "Disabled");
        } else {
            // On Android 12 and below, notifications are allowed by default
            tvNotificationStatus.setText("Enabled");
        }
    }

    private void loadQuizSummary() {
        SharedPreferences prefs = getActivity().getSharedPreferences("intake_quiz", Context.MODE_PRIVATE);

        if (!prefs.getBoolean("completed", false)) {
            quizSummaryCard.setVisibility(View.GONE);
            intakeQuizButton.setText("Take Intake Quiz");
            return;
        }

        intakeQuizButton.setText("Retake Intake Quiz");
        quizSummaryCard.setVisibility(View.VISIBLE);

        quizQ1.setText("Medication affecting cognition: " + yesNo(prefs, "q1"));
        quizQ2.setText("Neurological conditions: " + yesNo(prefs, "q2"));
        quizQ3.setText("Diagnosed with ADHD: " + yesNo(prefs, "q3"));
        quizQ4.setText("Regular alcohol consumption: " + yesNo(prefs, "q4"));
        quizQ5.setText("Regular tobacco/nicotine use: " + yesNo(prefs, "q5"));
        quizQ6.setText("Consumes a balanced diet: " + yesNo(prefs, "q6"));
        quizQ7.setText("Exercises regularly: " + yesNo(prefs, "q7"));
        quizQ8.setText("Gets 8+ hours of sleep: " + yesNo(prefs, "q8"));
    }

    private String yesNo(SharedPreferences prefs, String key) {
        return prefs.getBoolean(key, false) ? "Yes" : "No";
    }
}
