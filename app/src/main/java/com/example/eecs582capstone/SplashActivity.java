package com.example.eecs582capstone;

/*
Filename: SplashActivity.java
Author(s): Mya Hoersdig
Created: 02-14-2026
Last Modified: 03-05-2026
Overview and Purpose: Initiates the starting animation and then starts the registration page
Notes:
*/

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

/*
SplashActivity class: Creates Splash screen and displays it upon opening app
*/

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // On creation of the Activity -> show the splash screen
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen); //this is located in res > layout > splashscreen

        new Handler().postDelayed(
                () -> {
                    startActivity(new Intent(SplashActivity.this, Entry.class));
                    finish();
                }, 2000); //shows it for 2 seconds and then starts the activity
    }
}
