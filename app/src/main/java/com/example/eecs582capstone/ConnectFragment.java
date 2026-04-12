package com.example.eecs582capstone;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ConnectFragment extends Fragment {

    private View indicatorO1;
    private View indicatorO2;
    private View indicatorT3;
    private View indicatorT4;
    private TextView tvConnectStatus;

    public ConnectFragment() {
        // Required empty public constructor
    }

    private enum ElectrodeStatus {
        RED,
        YELLOW,
        GREEN
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_connect, container, false);

        indicatorO1 = root.findViewById(R.id.indicatorO1);
        indicatorO2 = root.findViewById(R.id.indicatorO2);
        indicatorT3 = root.findViewById(R.id.indicatorT3);
        indicatorT4 = root.findViewById(R.id.indicatorT4);
        tvConnectStatus = root.findViewById(R.id.tvConnectStatus);

        Bundle args = getArguments();
        if (args != null) {
            String deviceName = args.getString("device_name", "Unknown device");
            tvConnectStatus.setText("Selected: " + deviceName);
        } else {
            tvConnectStatus.setText("No device selected");
        }

        updateElectrodeIndicators(
                ElectrodeStatus.RED,
                ElectrodeStatus.RED,
                ElectrodeStatus.RED,
                ElectrodeStatus.RED
        );

        tvConnectStatus.setText("Not connected");

        return root;
    }

    private void testElectrodeIndicators() {
        setIndicatorColor(indicatorO1, "#31802b");   // green
        setIndicatorColor(indicatorO2, "#e09636");   // yellow
        setIndicatorColor(indicatorT3, "#c73d2e");   // red
        setIndicatorColor(indicatorT4, "#31802b");   // green

        tvConnectStatus.setText("Testing electrode indicators");
    }

    private void setIndicatorColor(View indicator, String colorHex) {
        GradientDrawable bg = (GradientDrawable) indicator.getBackground().mutate();
        bg.setColor(Color.parseColor(colorHex));
    }

    private void setIndicatorStatus(View indicator, ElectrodeStatus status) {
        int color;

        switch (status) {
            case GREEN:
                color = Color.parseColor("#31802b");
                break;
            case YELLOW:
                color = Color.parseColor("#e09636");
                break;
            case RED:
            default:
                color = Color.parseColor("#c73d2e");
                break;
        }

        GradientDrawable bg = (GradientDrawable) indicator.getBackground().mutate();
        bg.setColor(color);
    }

    private void updateElectrodeIndicators(ElectrodeStatus o1,
                                           ElectrodeStatus o2,
                                           ElectrodeStatus t3,
                                           ElectrodeStatus t4) {
        setIndicatorStatus(indicatorO1, o1);
        setIndicatorStatus(indicatorO2, o2);
        setIndicatorStatus(indicatorT3, t3);
        setIndicatorStatus(indicatorT4, t4);
    }
}
