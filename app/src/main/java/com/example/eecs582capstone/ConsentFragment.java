package com.example.eecs582capstone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConsentFragment extends Fragment {

    private CheckBox checkboxAgree;
    private Button buttonContinue;

    public ConsentFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.fragment_consent, container, false);

        checkboxAgree = view.findViewById(R.id.checkboxAgree);
        buttonContinue = view.findViewById(R.id.buttonContinue);

        buttonContinue.setEnabled(false);

        checkboxAgree.setOnCheckedChangeListener((buttonView, isChecked) -> {
            buttonContinue.setEnabled(isChecked);
        });

        buttonContinue.setOnClickListener(v -> {
            ConsentManager.setConsent(requireContext(), true);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        });

        return view;
    }
}