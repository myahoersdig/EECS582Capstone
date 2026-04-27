/*
Filename: ConsentManager.java
Author(s): Riley England
Created: 04-25-2026
Last Modified: 04-26-2026
Overview and Purpose: Sets & Stores consent value from user.
Notes: n/a
*/

package com.example.eecs582capstone;

import android.content.Context;
import android.content.SharedPreferences;

public class ConsentManager {

    private static final String PREF_NAME = "app_prefs";
    private static final String KEY_CONSENT = "eeg_consent_given";

    public static void setConsent(Context context, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_CONSENT, value).apply();
    }

    public static boolean hasConsent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_CONSENT, false);
    }
}
