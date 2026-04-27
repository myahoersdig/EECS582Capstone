/*
Filename: OnboardingFragment.java
Author(s): Abdelrahman Zeidan
Created: 04-25-2026
Last Modified: 04-26-2026
Overview and Purpose: Displays a single onboarding screen (image, title, description)
Notes:
*/

package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class OnboardingFragment extends Fragment {

    // Keys for passing data
    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESC = "arg_desc";

    // Creates new fragment instance with data
    public static OnboardingFragment newInstance(int imageRes, String title, String desc) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        fragment.setArguments(args);
        return fragment;
    }

    // Inflates layout and sets content
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_onboarding, container, false);

        ImageView imageView = view.findViewById(R.id.imageOnboarding);
        TextView titleView = view.findViewById(R.id.textTitle);
        TextView descView = view.findViewById(R.id.textDescription);

        // Set values from arguments
        if (getArguments() != null) {
            imageView.setImageResource(getArguments().getInt(ARG_IMAGE));
            titleView.setText(getArguments().getString(ARG_TITLE));
            descView.setText(getArguments().getString(ARG_DESC));
        }

        return view;
    }
}