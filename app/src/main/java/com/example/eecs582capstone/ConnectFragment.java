package com.example.eecs582capstone;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eecs582capstone.eeg.BrainBitManager;
import com.example.eecs582capstone.eeg.SelectedDeviceStore;
import com.neurosdk2.neuro.types.SignalChannelsData;
import com.neurosdk2.neuro.types.SensorInfo;

public class ConnectFragment extends Fragment {

    // Resistance thresholds in Ohms (tunable once real values are observed)
    private static final double RESIST_GREEN_MAX  =  5_000.0;  // < 5 kΩ  → good contact
    private static final double RESIST_YELLOW_MAX = 15_000.0;  // < 15 kΩ → acceptable

    private View indicatorO1;
    private View indicatorO2;
    private View indicatorT3;
    private View indicatorT4;
    private TextView tvConnectStatus;

    private BrainBitManager brainBitManager;
    private String selectedDeviceName = "Unknown device";

    public ConnectFragment() {}

    private enum ElectrodeStatus {
        RED, YELLOW, GREEN
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

        setInitialStatus();

        Bundle args = getArguments();
        if (args != null) {
            selectedDeviceName = args.getString("device_name", "Unknown device");
        }

        tvConnectStatus.setText("Selected: " + selectedDeviceName);

        brainBitManager = new BrainBitManager(requireContext(), new BrainBitManager.Listener() {
            @Override
            public void onStateChanged(BrainBitManager.ConnectionState state, String message) {
                if (!isAdded()) return;

                switch (state) {
                    case CONNECTING:
                        tvConnectStatus.setText("Connecting to " + selectedDeviceName + "...");
                        break;

                    case CONNECTED:
                        tvConnectStatus.setText("Connected to " + selectedDeviceName);
                        brainBitManager.startElectrodeMonitoring();
                        brainBitManager.startSignal();
                        break;

                    case DISCONNECTED:
                        tvConnectStatus.setText("Disconnected");
                        setInitialStatus();
                        break;

                    case ERROR:
                        tvConnectStatus.setText("Connection failed");
                        setInitialStatus();
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                        break;
                }
            }

            @Override
            public void onSignalStarted() {}

            @Override
            public void onSignalStopped() {}

            @Override
            public void onSignalDataReceived(SignalChannelsData[] data) {}

            @Override
            public void onResistanceReceived(double o1Ohm, double o2Ohm,
                                             double t3Ohm, double t4Ohm) {
                if (!isAdded()) return;
                updateElectrodeIndicators(
                        resistToStatus(o1Ohm),
                        resistToStatus(o2Ohm),
                        resistToStatus(t3Ohm),
                        resistToStatus(t4Ohm)
                );
            }
        });

        SensorInfo selectedDevice = SelectedDeviceStore.getSelectedDevice();
        if (selectedDevice == null) {
            tvConnectStatus.setText("No device selected");
        } else {
            brainBitManager.connectToDevice(selectedDevice);
        }

        return root;
    }

    private ElectrodeStatus resistToStatus(double ohms) {
        if (Double.isInfinite(ohms) || Double.isNaN(ohms) || ohms > RESIST_YELLOW_MAX) {
            return ElectrodeStatus.RED;
        } else if (ohms > RESIST_GREEN_MAX) {
            return ElectrodeStatus.YELLOW;
        } else {
            return ElectrodeStatus.GREEN;
        }
    }

    private void setInitialStatus() {
        setIndicatorStatus(indicatorO1, ElectrodeStatus.RED);
        setIndicatorStatus(indicatorO2, ElectrodeStatus.RED);
        setIndicatorStatus(indicatorT3, ElectrodeStatus.RED);
        setIndicatorStatus(indicatorT4, ElectrodeStatus.RED);
    }

    private void setIndicatorStatus(View indicator, ElectrodeStatus status) {
        int color;
        switch (status) {
            case GREEN:  color = Color.parseColor("#31802b"); break;
            case YELLOW: color = Color.parseColor("#e09636"); break;
            case RED:
            default:     color = Color.parseColor("#c73d2e"); break;
        }
        GradientDrawable bg = (GradientDrawable) indicator.getBackground().mutate();
        bg.setColor(color);
    }

    private void updateElectrodeIndicators(ElectrodeStatus o1, ElectrodeStatus o2,
                                           ElectrodeStatus t3, ElectrodeStatus t4) {
        setIndicatorStatus(indicatorO1, o1);
        setIndicatorStatus(indicatorO2, o2);
        setIndicatorStatus(indicatorT3, t3);
        setIndicatorStatus(indicatorT4, t4);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (brainBitManager != null) {
            brainBitManager.release();
            brainBitManager = null;
        }
    }
}
