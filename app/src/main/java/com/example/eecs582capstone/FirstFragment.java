package com.example.eecs582capstone;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FirstFragment extends Fragment {

    private TextView sessionStatus;
    private Button btnStartSession, btnEndSession;
    private dbConnect dbHelper;
    private int currentUserId = -1;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_first, container, false);

        sessionStatus = view.findViewById(R.id.sessionStatus);
        btnStartSession = view.findViewById(R.id.btnStartSession);
        btnEndSession = view.findViewById(R.id.btnEndSession);

        dbHelper = new dbConnect(getActivity());

        SharedPreferences prefs = getActivity().getSharedPreferences("user_session", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("email", null);

        if (userEmail != null) {
            Users user = dbHelper.getUserByEmail(userEmail);
            if (user != null) {
                currentUserId = user.getId();
            }
        }

        updateSessionUI();

        btnStartSession.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            long sessionId = dbHelper.startSession(currentUserId);
            if (sessionId != -1) {
                Toast.makeText(getActivity(), "Session started", Toast.LENGTH_SHORT).show();
                updateSessionUI();
            } else {
                Toast.makeText(getActivity(), "Failed to start session", Toast.LENGTH_SHORT).show();
            }
        });

        btnEndSession.setOnClickListener(v -> {
            if (currentUserId == -1) {
                Toast.makeText(getActivity(), "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            long activeId = dbHelper.getActiveSessionId(currentUserId);
            if (activeId != -1) {
                dbHelper.endSession(activeId);
                Toast.makeText(getActivity(), "Session ended", Toast.LENGTH_SHORT).show();
                updateSessionUI();
            } else {
                Toast.makeText(getActivity(), "No active session", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void updateSessionUI() {
        if (currentUserId == -1) {
            sessionStatus.setText("Please log in");
            btnStartSession.setEnabled(false);
            btnEndSession.setEnabled(false);
            return;
        }

        boolean active = dbHelper.hasActiveSession(currentUserId);
        if (active) {
            sessionStatus.setText("Session in progress");
            btnStartSession.setEnabled(false);
            btnEndSession.setEnabled(true);
        } else {
            sessionStatus.setText("No active session");
            btnStartSession.setEnabled(true);
            btnEndSession.setEnabled(false);
        }
    }
}