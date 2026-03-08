package com.example.eecs582capstone;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class UserIntakeQuizActivity extends AppCompatActivity {

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
        SharedPreferences prefs = getSharedPreferences("intake_quiz", MODE_PRIVATE);
        if (prefs.getBoolean("completed", false)) {
            restoreAnswers(prefs);
        }

        MaterialButton submit = findViewById(R.id.btn_submit);
        submit.setOnClickListener(v -> submitQuiz());
    }

    private void restoreAnswers(SharedPreferences prefs) {
        setGroup(q1, prefs.getBoolean("q1", false));
        setGroup(q2, prefs.getBoolean("q2", false));
        setGroup(q3, prefs.getBoolean("q3", false));
        setGroup(q4, prefs.getBoolean("q4", false));
        setGroup(q5, prefs.getBoolean("q5", false));
        setGroup(q6, prefs.getBoolean("q6", false));
        setGroup(q7, prefs.getBoolean("q7", false));
        setGroup(q8, prefs.getBoolean("q8", false));
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

        SharedPreferences.Editor editor = getSharedPreferences("intake_quiz", MODE_PRIVATE).edit();
        editor.putBoolean("completed", true);
        editor.putBoolean("q1", q1.getCheckedRadioButtonId() == R.id.q1_yes);
        editor.putBoolean("q2", q2.getCheckedRadioButtonId() == R.id.q2_yes);
        editor.putBoolean("q3", q3.getCheckedRadioButtonId() == R.id.q3_yes);
        editor.putBoolean("q4", q4.getCheckedRadioButtonId() == R.id.q4_yes);
        editor.putBoolean("q5", q5.getCheckedRadioButtonId() == R.id.q5_yes);
        editor.putBoolean("q6", q6.getCheckedRadioButtonId() == R.id.q6_yes);
        editor.putBoolean("q7", q7.getCheckedRadioButtonId() == R.id.q7_yes);
        editor.putBoolean("q8", q8.getCheckedRadioButtonId() == R.id.q8_yes);
        editor.apply();

        Toast.makeText(this, "Quiz submitted!", Toast.LENGTH_SHORT).show();
        finish();
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
