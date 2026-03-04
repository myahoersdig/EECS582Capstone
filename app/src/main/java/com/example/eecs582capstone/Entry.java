package com.example.eecs582capstone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class Entry extends Activity {
    static String TAG = "Look at this"; //you can add this to error catch statements to check in logcat
    EditText  edtEmailAddressLog, edtPasswordLog;
    Button btnLogin, btnRegisterLog;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.login);

        edtEmailAddressLog = findViewById(R.id.editEmailAddressLog);
        edtPasswordLog = findViewById(R.id.edtPasswordLog);
        btnLogin = findViewById(R.id.btnLoginLog);
        btnRegisterLog= findViewById(R.id.btnRegisterLog);


        btnRegisterLog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(Entry.this,register.class);
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

                dbConnect dbHelper = new dbConnect(Entry.this);
                boolean valid = dbHelper.checkUser(email, password);

                if (valid) {
                    Toast.makeText(Entry.this, "Login successful", Toast.LENGTH_SHORT).show();
                    // TODO: Start your main activity here
                    // Intent intent = new Intent(Entry.this, MainActivity.class);
                    // startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(Entry.this, "Invalid email or password", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}