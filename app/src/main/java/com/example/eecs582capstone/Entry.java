package com.example.eecs582capstone;

/*
Filename: Entry.java
Author(s): Mya Hoersdig, Abdelrahman Zeidan
Created: 02-14-2026
Last Modified: 03-05-2026
Overview and Purpose: Entry initializes the log in page and authenticates. After authentication,
it launches MainActivty, which hosts the main app functions.
Notes: Needs to be adapted to use the cloud authentication database when possible
*/

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/*
Entry class: 
*/
public class Entry extends Activity {
    //Entry Activity for Attune
    static String TAG = "ENTRY";
    EditText edtEmailAddressLog, edtPasswordLog;
    Button btnLogin, btnRegisterLog;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.login);

        edtEmailAddressLog = findViewById(R.id.editEmailAddressLog);
        edtPasswordLog = findViewById(R.id.edtPasswordLog);
        btnLogin = findViewById(R.id.btnLoginLog);
        btnRegisterLog = findViewById(R.id.btnRegisterLog);

        btnRegisterLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(Entry.this, register.class);
                startActivity(i);
            }
        });
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmailAddressLog.getText().toString().trim();
                String password = edtPasswordLog.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Entry.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }

                btnLogin.setEnabled(false);
                btnRegisterLog.setEnabled(false);

                SupabaseAuthClient.signIn(email, password, new SupabaseAuthClient.AuthCallback() {
                    @Override
                    public void onSuccess(String email, String firstName, String lastName) {
                        dbConnect dbHelper = new dbConnect(Entry.this);
                        dbHelper.ensureLocalUser(email, firstName, lastName);

                        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                        prefs.edit()
                                .putString("email", email)
                                .putBoolean("logged_in", true)
                                .apply();

                        SharedPreferences onboardingPrefs = getSharedPreferences(OnboardingActivity.PREFS_NAME, MODE_PRIVATE);
                        boolean hasSeenOnboarding = onboardingPrefs.getBoolean(
                                OnboardingActivity.getOnboardingKey(email), false);

                        Intent intent = hasSeenOnboarding
                                ? new Intent(Entry.this, MainActivity.class)
                                : new Intent(Entry.this, OnboardingActivity.class)
                                        .putExtra(OnboardingActivity.EXTRA_USER_EMAIL, email);

                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(Entry.this, message, Toast.LENGTH_LONG).show();
                        btnLogin.setEnabled(true);
                        btnRegisterLog.setEnabled(true);
                    }
                });
            }
        });

        //Already logged in
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean loggedIn = prefs.getBoolean("logged_in", false);
        if (loggedIn) {
            String email = prefs.getString("email", "");

            SharedPreferences onboardingPrefs = getSharedPreferences(OnboardingActivity.PREFS_NAME, MODE_PRIVATE);
            boolean hasSeenOnboarding = onboardingPrefs.getBoolean(
                    OnboardingActivity.getOnboardingKey(email),
                    false
            );

            Intent intent;
            if (hasSeenOnboarding) {
                intent = new Intent(Entry.this, MainActivity.class);
            } else {
                intent = new Intent(Entry.this, OnboardingActivity.class);
                intent.putExtra(OnboardingActivity.EXTRA_USER_EMAIL, email);
            }

            startActivity(intent);
            finish();
        }
    }
}