package com.example.eecs582capstone;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "attune_prefs";
    public static final String EXTRA_USER_EMAIL = "user_email";

    private ViewPager2 viewPager;
    private Button btnNext, btnSkip;
    private String userEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        userEmail = getIntent().getStringExtra(EXTRA_USER_EMAIL);

        viewPager = findViewById(R.id.viewPagerOnboarding);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);

        List<OnboardingItem> items = new ArrayList<>();
        items.add(new OnboardingItem(
                R.drawable.ic_launcher_foreground,
                "Welcome to Attune",
                "Track and improve your focus sessions with personalized insights."
        ));
        items.add(new OnboardingItem(
                R.drawable.ic_launcher_foreground,
                "Connect Your EEG Device",
                "Scan for your BrainBit headset and connect before beginning a session."
        ));
        items.add(new OnboardingItem(
                R.drawable.ic_launcher_foreground,
                "Complete the Intake Quiz",
                "Answer a short quiz so Attune can better understand your focus traits."
        ));
        items.add(new OnboardingItem(
                R.drawable.ic_launcher_foreground,
                "Start Sessions and Review Results",
                "Begin a focus session, record notes, and review your results over time."
        ));

        OnboardingAdapter adapter = new OnboardingAdapter(this, items);
        viewPager.setAdapter(adapter);

        btnSkip.setOnClickListener(v -> finishOnboarding());

        btnNext.setOnClickListener(v -> {
            int current = viewPager.getCurrentItem();
            if (current < items.size() - 1) {
                viewPager.setCurrentItem(current + 1, true);
            } else {
                finishOnboarding();
            }
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == items.size() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });
    }

    private void finishOnboarding() {
        if (userEmail != null && !userEmail.trim().isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().putBoolean(getOnboardingKey(userEmail), true).apply();
        }

        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public static String getOnboardingKey(String email) {
        return "onboarding_seen_" + email.trim().toLowerCase();
    }
}