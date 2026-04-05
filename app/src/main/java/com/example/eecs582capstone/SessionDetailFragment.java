package com.example.eecs582capstone;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Date;

public class SessionDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "session_id";

    public static SessionDetailFragment newInstance(long sessionId) {
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    private long sessionId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_session_detail, container, false);

        // UI references
        TextView tvSleep = view.findViewById(R.id.tvSleep);
        TextView tvMeal = view.findViewById(R.id.tvMeal);
        TextView tvCaffeine = view.findViewById(R.id.tvCaffeine);
        TextView tvMood = view.findViewById(R.id.tvMood);
        TextView tvStress = view.findViewById(R.id.tvStress);
        TextView tvLocation = view.findViewById(R.id.tvLocation);
        TextView tvGenre = view.findViewById(R.id.tvGenre);
        TextView tvLyrics = view.findViewById(R.id.tvLyrics);
        TextView tvTempo = view.findViewById(R.id.tvTempo);
        TextView tvDateTime = view.findViewById(R.id.tvDateTime);
        ProgressBar pbLight = view.findViewById(R.id.pbLight);
        ProgressBar pbNoise = view.findViewById(R.id.pbNoise);
        ProgressBar pbFamiliarity = view.findViewById(R.id.pbFamiliarity);
        Button btnBack = view.findViewById(R.id.btnBack);
        Button btnDelete = view.findViewById(R.id.btnDeleteSession);

        sessionId = getArguments() != null ? getArguments().getLong(ARG_SESSION_ID) : -1;

        // Load session from DB instead of mock data
        dbConnect dbHelper = new dbConnect(requireContext());
        Cursor cursor = dbHelper.getSavedSessionById(sessionId);

        try {
            if (cursor.moveToFirst()) {
                tvSleep.setText(safeText(cursor, "sleep_hours"));
                tvMeal.setText(safeText(cursor, "meal_info"));
                tvCaffeine.setText(safeText(cursor, "caffeine"));
                tvMood.setText(safeText(cursor, "mood"));
                tvStress.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("stress"))));
                tvLocation.setText(safeText(cursor, "location"));
                tvGenre.setText(safeText(cursor, "music_genre"));
                tvLyrics.setText(safeText(cursor, "lyrics_preference"));
                tvTempo.setText(String.valueOf(cursor.getInt(cursor.getColumnIndexOrThrow("tempo_bpm"))));

                // Format the stored timestamp into a readable date/time string
                long startTimeMillis = cursor.getLong(cursor.getColumnIndexOrThrow("start_time"));
                Date sessionDate = new Date(startTimeMillis);
                String formattedDate = DateFormat.format("MMM dd, yyyy - h:mm a", sessionDate).toString();
                tvDateTime.setText(formattedDate);

                pbLight.setProgress(cursor.getInt(cursor.getColumnIndexOrThrow("light_level")));
                pbNoise.setProgress(cursor.getInt(cursor.getColumnIndexOrThrow("noise_level")));
                pbFamiliarity.setProgress(cursor.getInt(cursor.getColumnIndexOrThrow("familiarity")));
            }
        } finally {
            cursor.close();
        }

        btnBack.setOnClickListener(v -> getParentFragmentManager().popBackStack());

        // DELETE WITH CONFIRMATION
        btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Session")
                    .setMessage("Are you sure you want to delete this session?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.deleteSession(sessionId);
                        Toast.makeText(getContext(), "Session deleted", Toast.LENGTH_SHORT).show();
                        getParentFragmentManager().popBackStack();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        return view;
    }

    private String safeText(Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }
}