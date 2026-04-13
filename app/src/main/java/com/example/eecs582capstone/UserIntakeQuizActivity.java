package com.example.eecs582capstone;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class UserIntakeQuizActivity extends AppCompatActivity {

    public static final String QUIZ_PREFS = "intake_quiz";
    public static final String KEY_COMPLETED = "completed";
    public static final String KEY_Q1 = "q1";
    public static final String KEY_Q2 = "q2";
    public static final String KEY_Q3 = "q3";
    public static final String KEY_Q4 = "q4";
    public static final String KEY_Q5 = "q5";
    public static final String KEY_Q6 = "q6";
    public static final String KEY_Q7 = "q7";
    public static final String KEY_Q8 = "q8";

    private RadioGroup q1, q2, q3, q4, q5, q6, q7, q8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intake_form);

        q1 = findViewById(R.id.q1_group);
        q2 = findViewById(R.id.q2_group);
        q3 = findViewById(R.id.q3_group);
        q4 = findViewById(R.id.q4_group);
        q5 = findViewById(R.id.q5_group);
        q6 = findViewById(R.id.q6_group);
        q7 = findViewById(R.id.q7_group);
        q8 = findViewById(R.id.q8_group);

        // Pre-fill answers if the quiz was previously completed
        SharedPreferences prefs = getSharedPreferences(QUIZ_PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(KEY_COMPLETED, false)) {
            restoreAnswers(prefs);
        }

        MaterialButton submit = findViewById(R.id.btn_submit);
        submit.setOnClickListener(v -> submitQuiz());
    }

    private void restoreAnswers(SharedPreferences prefs) {
        setGroup(q1, prefs.getBoolean(KEY_Q1, false));
        setGroup(q2, prefs.getBoolean(KEY_Q2, false));
        setGroup(q3, prefs.getBoolean(KEY_Q3, false));
        setGroup(q4, prefs.getBoolean(KEY_Q4, false));
        setGroup(q5, prefs.getBoolean(KEY_Q5, false));
        setGroup(q6, prefs.getBoolean(KEY_Q6, false));
        setGroup(q7, prefs.getBoolean(KEY_Q7, false));
        setGroup(q8, prefs.getBoolean(KEY_Q8, false));
    }

    /** Checks the Yes or No radio button in the given group. */
    private void setGroup(RadioGroup group, boolean isYes) {
        group.check(group.getChildAt(isYes ? 0 : 1).getId());
    }

    private void submitQuiz() {
        if (!allAnswered()) {
            Toast.makeText(this, "Please answer all questions before submitting.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences(QUIZ_PREFS, MODE_PRIVATE).edit();
        editor.putBoolean(KEY_COMPLETED, true);
        editor.putBoolean(KEY_Q1, q1.getCheckedRadioButtonId() == R.id.q1_yes);
        editor.putBoolean(KEY_Q2, q2.getCheckedRadioButtonId() == R.id.q2_yes);
        editor.putBoolean(KEY_Q3, q3.getCheckedRadioButtonId() == R.id.q3_yes);
        editor.putBoolean(KEY_Q4, q4.getCheckedRadioButtonId() == R.id.q4_yes);
        editor.putBoolean(KEY_Q5, q5.getCheckedRadioButtonId() == R.id.q5_yes);
        editor.putBoolean(KEY_Q6, q6.getCheckedRadioButtonId() == R.id.q6_yes);
        editor.putBoolean(KEY_Q7, q7.getCheckedRadioButtonId() == R.id.q7_yes);
        editor.putBoolean(KEY_Q8, q8.getCheckedRadioButtonId() == R.id.q8_yes);
        editor.apply();

        Toast.makeText(this, "Quiz submitted!", Toast.LENGTH_SHORT).show();
        finish();
    }

    // Helper method used by other screens when the user wants to remove the saved survey data
    public static void clearSavedQuiz(Context context) {
        context.getSharedPreferences(QUIZ_PREFS, MODE_PRIVATE).edit().clear().apply();
    }

    private boolean allAnswered() {
        return q1.getCheckedRadioButtonId() != -1
                && q2.getCheckedRadioButtonId() != -1
                && q3.getCheckedRadioButtonId() != -1
                && q4.getCheckedRadioButtonId() != -1
                && q5.getCheckedRadioButtonId() != -1
                && q6.getCheckedRadioButtonId() != -1
                && q7.getCheckedRadioButtonId() != -1
                && q8.getCheckedRadioButtonId() != -1;
    }
}