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
        // NEW: Login button click
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = edtEmailAddressLog.getText().toString().trim();
                String password = edtPasswordLog.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(Entry.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                    return;
                }
                //connect to authentication DB
                dbConnect dbHelper = new dbConnect(Entry.this);
                //checks validity of login
                boolean valid = dbHelper.checkUser(email, password);

                if (valid) {
                    Toast.makeText(Entry.this, "Login successful", Toast.LENGTH_SHORT).show();
                    //creates shared preferences for the user session
                    SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("email", email); // this email will be used to query db in Profile
                    editor.putBoolean("logged_in", true);
                    editor.apply();

                    Intent intent = new Intent(Entry.this, MainActivity.class);
                    startActivity(intent); //Start main activity
                    finish();
                } else {
                    //Wrong email/password
                    Toast.makeText(Entry.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Already logged in
        SharedPreferences prefs = getSharedPreferences("user_session", MODE_PRIVATE);
        boolean loggedIn = prefs.getBoolean("logged_in", false);
        if (loggedIn) {
            Intent intent = new Intent(Entry.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
