/*
Filename: OnboardingActivity.java
Author(s): Abdelrahman Zeidan
Created:04-25-2026
Last Modified: 04-26-2026
Overview and Purpose: Controls onboarding flow using swipeable screens and navigates user to main app
Notes:
*/

package com.example.eecs582capstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    // Stores shared preference keys
    public static final String PREFS_NAME = "attune_prefs";
    public static final String EXTRA_USER_EMAIL = "user_email";

    // UI elements
    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        // Get user email from previous activity
        userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);

        // Initialize UI components
        viewPager = findViewById(R.id.viewPagerOnboarding);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        // Create onboarding screens
        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(R.drawable.ic_launcher_foreground,
                "Welcome to Attune",
                "Track and improve your focus sessions."));
        items.add(new OnboardingItem(R.drawable.ic_launcher_foreground,
                "Connect Device",
                "Connect your EEG headset."));
        items.add(new OnboardingItem(R.drawable.ic_launcher_foreground,
                "Take Quiz",
                "Answer questions for personalization."));
        items.add(new OnboardingItem(R.drawable.ic_launcher_foreground,
                "Start Sessions",
                "Track and review your progress."));

        // Set adapter for swipe pages
        OnboardingAdapter adapter = new OnboardingAdapter(this, items);
        viewPager.setAdapter(adapter);

        // Skip button ends onboarding
        btnSkip.setOnClickListener(v -> finishOnboarding());

        // Next button moves pages or finishes
        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < items.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        // Change button text on last page
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position == items.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });
    }

    // Saves onboarding status and moves to main screen
    private void finishOnboarding() {
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(getOnboardingKey(userEmail), true).apply();
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    // Generates unique onboarding key per user
    public static String getOnboardingKey(String email) {
        return "onboarding_seen_" + email.trim().toLowerCase();
    }
}