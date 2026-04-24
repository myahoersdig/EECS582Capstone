package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class OnboardingFragment extends Fragment {

    private static final String ARG_IMAGE = "arg_image";
    private static final String ARG_TITLE = "arg_title";
    private static final String ARG_DESC = "arg_desc";

    public static OnboardingFragment newInstance(int imageRes, String title, String desc) {
        OnboardingFragment fragment = new OnboardingFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE, imageRes);
        args.putString(ARG_TITLE, title);
        args.putString(ARG_DESC, desc);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_onboarding, container, false);

        ImageView imageView = view.findViewById(R.id.imageOnboarding);
        TextView titleView = view.findViewById(R.id.textTitle);
        TextView descView = view.findViewById(R.id.textDescription);

        if (getArguments() != null) {
            imageView.setImageResource(getArguments().getInt(ARG_IMAGE));
            titleView.setText(getArguments().getString(ARG_TITLE));
            descView.setText(getArguments().getString(ARG_DESC));
        }

        return view;
    }
}