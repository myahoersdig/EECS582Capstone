package com.example.eecs582capstone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

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

    }
}
