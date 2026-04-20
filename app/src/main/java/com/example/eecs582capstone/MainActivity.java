
package com.example.eecs582capstone;

/*
Filename: MainActivity.java
Author(s): Mya Hoersdig, Jackson Yanek
Created: 03-05-2026
Last Modified: 04-19-2026
Overview and Purpose: Initializes the BottomNavigationView and loads the corresponding fragments the icon is selected
Notes: 
fragments displayed are:
    FirstFragment (Home)
    SecondFragment (Profile)
    ThirdFragment (Upload)
*/

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/*
MainActivity class: 
*/

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        Fragment homeFragment = new HomeFragment();
        Fragment profileFragment = new ProfileFragment();
        Fragment resultsFragment = new ResultsFragment();

        setCurrentFragment(homeFragment);
        bottomNavigationView.setSelectedItemId(R.id.home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                setCurrentFragment(homeFragment);
            } else if (id == R.id.profile) {
                setCurrentFragment(profileFragment);
            } else if (id == R.id.upload) {
                setCurrentFragment(resultsFragment);
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
