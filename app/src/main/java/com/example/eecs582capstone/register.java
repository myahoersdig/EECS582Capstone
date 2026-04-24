package com.example.eecs582capstone;

/*
Filename: register.java
Author(s): Abdelrahman Zeidan
Created: 02-24-2026
Last Modified: 03-01-2026
Overview and Purpose: Handles the Register page for Attune
Notes: Needs to be updated to use cloud authentication eventually
*/

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


/*
register class:
*/

public class register extends AppCompatActivity {

    private EditText edtFirstNameReg, edtLastNameReg, edtEmailAddressReg, edtPasswordReg, edtConfirmPasswordReg;
    private Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        edtFirstNameReg = findViewById(R.id.edtFirstNameReg);
        edtLastNameReg = findViewById(R.id.edtLastNameReg);
        edtEmailAddressReg = findViewById(R.id.edtEmailAddressReg);
        edtPasswordReg = findViewById(R.id.edtPasswordReg);
        edtConfirmPasswordReg = findViewById(R.id.edtConfirmPasswordReg);
        btnRegister = findViewById(R.id.btnRegisterLog);

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = edtFirstNameReg.getText().toString().trim();
                String lastName = edtLastNameReg.getText().toString().trim();
                String email = edtEmailAddressReg.getText().toString().trim();
                String password = edtPasswordReg.getText().toString().trim();
                String confirmPassword = edtConfirmPasswordReg.getText().toString().trim();

                // Check for empty fields
                if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(register.this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if passwords match
                if (!password.equals(confirmPassword)) {
                    Toast.makeText(register.this, "Passwords do not match", Toast.LENGTH_SHORT).show();
                    edtConfirmPasswordReg.setText("");
                    return;
                }

                // Save to database
                dbConnect dbHelper = new dbConnect(register.this);
                Users newUser = new Users(0, firstName, lastName, email, password); // id is auto-generated
                dbHelper.addUser(newUser);

                SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putString("email", email);
                editor.putBoolean("logged_in", true);
                editor.apply();

                Toast.makeText(register.this, "Registration successful", Toast.LENGTH_SHORT).show();

                Intent intent = new Intent(register.this, OnboardingActivity.class);
                intent.putExtra(OnboardingActivity.EXTRA_USER_EMAIL, email);
                startActivity(intent);
                finish(); // Move to onboarding screen
            }
        });
    }
}