/*
MainActivity.java by Mya Hoersdig

initializes the BottomNavigationView and loads the corresponding fragments the icon is selected

fragments displayed are:
    FirstFragment (Home)
    SecondFragment (Profile)
    ThirdFragment (Upload)

ensure that the first page to show is the profile page
 */
package com.example.eecs582capstone;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment firstFragment = new FirstFragment();
        Fragment secondFragment = new SecondFragment();
        Fragment thirdFragment = new ThirdFragment();

        setCurrentFragment(secondFragment);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                setCurrentFragment(firstFragment);
            } else if (id == R.id.profile) {
                setCurrentFragment(secondFragment);
            } else if (id == R.id.upload) {
                setCurrentFragment(thirdFragment);
            }
            return true;
        });
    }

    private void setCurrentFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.flFragment, fragment)
                .commit();
    }
}