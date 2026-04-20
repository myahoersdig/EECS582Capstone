/*
 * Manu Redd
 * April 19th
 * SessionDetailFragment.java
 *
 *
 * This fragment displays the detailed information of a specific user session.
 * It retrieves session data (such as sleep, mood, metrics, and music preferences)
 * from the database and allows the user to view, edit, or delete session notes,
 * as well as delete the session itself.
 */

package com.example.eecs582capstone;

import android.app.AlertDialog;
import android.database.Cursor;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Date;

public class SessionDetailFragment extends Fragment {

    private static final String ARG_SESSION_ID = "session_id";

    // Creates a new instance of the fragment with the specified session ID.
    public static SessionDetailFragment newInstance(long sessionId) {
        SessionDetailFragment fragment = new SessionDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_SESSION_ID, sessionId);
        fragment.setArguments(args);
        return fragment;
    }

    private long sessionId;

    // Inflates the layout, initializes UI components, and loads session data from the database.
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
        TextView tvSessionNotes = view.findViewById(R.id.tvSessionNotes);
        Button btnEditNote = view.findViewById(R.id.btnEditNote);
        Button btnDeleteNote = view.findViewById(R.id.btnDeleteNote);
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

                String notes = cursor.getString(cursor.getColumnIndexOrThrow("session_notes"));
                tvSessionNotes.setText(notes != null && !notes.trim().isEmpty() ? notes : "No notes");
            }
        } finally {
            cursor.close();
        }

        btnEditNote.setOnClickListener(v -> showEditNoteDialog(dbHelper, tvSessionNotes));

        btnDeleteNote.setOnClickListener(v -> {
            String current = tvSessionNotes.getText().toString();
            if ("No notes".equals(current)) {
                Toast.makeText(getContext(), "No note to delete", Toast.LENGTH_SHORT).show();
                return;
            }
            new AlertDialog.Builder(requireContext())
                    .setTitle("Delete Note")
                    .setMessage("Remove the note for this session?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        dbHelper.updateSessionNotes(sessionId, null);
                        tvSessionNotes.setText("No notes");
                        Toast.makeText(getContext(), "Note deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

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

    // Displays an AlertDialog with an EditText to allow the user to update session notes.
    private void showEditNoteDialog(dbConnect dbHelper, TextView tvSessionNotes) {
        EditText editText = new EditText(requireContext());
        editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(500)});
        editText.setHint("Enter session notes (max 500 characters)");
        editText.setMinLines(3);
        editText.setGravity(android.view.Gravity.TOP);

        String existing = tvSessionNotes.getText().toString();
        if (!"No notes".equals(existing)) {
            editText.setText(existing);
            editText.setSelection(existing.length());
        }

        // Char counter label
        TextView charCount = new TextView(requireContext());
        charCount.setText(editText.getText().length() + " / 500");
        charCount.setTextSize(12);
        charCount.setGravity(android.view.Gravity.END);
        charCount.setPadding(0, 0, 8, 0);

        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                charCount.setText(s.length() + " / 500");
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);
        layout.addView(editText);
        layout.addView(charCount);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Session Note")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newNote = editText.getText().toString().trim();
                    dbHelper.updateSessionNotes(sessionId, newNote.isEmpty() ? null : newNote);
                    tvSessionNotes.setText(newNote.isEmpty() ? "No notes" : newNote);
                    Toast.makeText(getContext(), "Note saved", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // Helper method to extract text from a cursor and return "N/A" if the value is null or empty.
    private String safeText(Cursor cursor, String columnName) {
        String value = cursor.getString(cursor.getColumnIndexOrThrow(columnName));
        return value == null || value.trim().isEmpty() ? "N/A" : value;
    }
}
