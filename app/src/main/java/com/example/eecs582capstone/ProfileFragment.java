/*
SecondFragment.java by Mya Hoersdig

initializes the second fragment (profile)
 */

package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.widget.TextView;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Button;
import android.content.Intent;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView username = view.findViewById(R.id.profileUsername);
        TextView emailText = view.findViewById(R.id.profileEmail);

        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("email", null);

        if (userEmail != null) {
            dbConnect db = new dbConnect(getActivity());
            Users user = db.getUserByEmail(userEmail);
            if (user != null) {
                username.setText(user.getFirstname() + " " + user.getLastname());
                emailText.setText(user.getEmailAddress());
            }
        }

        // Logout button (optional)
        Button logout = view.findViewById(R.id.logoutButton);
        logout.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(getActivity(), Entry.class);
            startActivity(intent);
            getActivity().finish();
        });

        return view;
    }
}