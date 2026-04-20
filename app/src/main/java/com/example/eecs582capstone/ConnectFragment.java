package com.example.eecs582capstone;

/*
Filename: ConnectFragment.java
Author(s): Riley England
Created: 04-11-2026
Last Modified: 04-19-2026
Overview and Purpose: Handles the UI and logic for connecting to a selected EEG device,
monitoring signal quality from electrodes, and guiding the user through the connection
process before proceeding to the main application.
Notes: This fragment initializes the BrainBitManager, connects to the selected EEG device,
and listens for signal data updates. It processes incoming EEG signal values to estimate
electrode quality and updates UI indicators accordingly. The fragment ensures that all
electrodes are in a stable (green) state before allowing the user to continue. It also
bridges between device selection (SelectedDeviceStore) and active connection management
(BrainBitConnectionStore).
*/

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.eecs582capstone.eeg.BrainBitManager;
import com.example.eecs582capstone.eeg.SelectedDeviceStore;
import com.example.eecs582capstone.eeg.BrainBitConnectionStore;
import com.neurosdk2.neuro.types.SignalChannelsData;
import com.neurosdk2.neuro.types.SensorInfo;

/*
ConnectFragment class: A UI controller responsible for managing the EEG device connection process,
displaying real-time electrode signal quality, and allowing the user to proceed once all
electrodes are properly connected.
*/

public class ConnectFragment extends Fragment {

    private View indicatorO1;
    private View indicatorO2;
    private View indicatorT3;
    private View indicatorT4;
    private TextView tvConnectStatus;
    private Button btnContinueToHome;
    private BrainBitManager brainBitManager;
    private String selectedDeviceName = "Unknown device";

    public ConnectFragment() {}

    private boolean allElectrodesReady = false;

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
        btnContinueToHome = root.findViewById(R.id.btnContinueToHome);
        btnContinueToHome.setVisibility(View.GONE);
        setInitialStatus();

        Bundle args = getArguments();
        if (args != null) {
            selectedDeviceName = args.getString("device_name", "Unknown device");
        }

        tvConnectStatus.setText("Selected: " + selectedDeviceName);
        btnContinueToHome.setOnClickListener(v -> {
            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, new HomeFragment())
                    .addToBackStack(null)
                    .commit();
        });

        brainBitManager = new BrainBitManager(requireContext(), new BrainBitManager.Listener() {
            @Override
            public void onStateChanged(BrainBitManager.ConnectionState state, String message) {
                if (!isAdded()) return;
                android.util.Log.d("ConnectFragment", "onStateChanged: " + state + " msg=" + message);

                switch (state) {
                    case CONNECTING:
                        tvConnectStatus.setText("Connecting to " + selectedDeviceName + "...");
                        break;

                    case CONNECTED:
                        tvConnectStatus.setText("Connected to " + selectedDeviceName);
                        android.util.Log.d("ConnectFragment", "Starting signal");
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
            public void onSignalStarted() {
                android.util.Log.d("ConnectFragment", "onSignalStarted");
            }

            @Override
            public void onSignalStopped() {
                android.util.Log.d("ConnectFragment", "onSignalStopped");
            }

            @Override
            public void onSignalDataReceived(SignalChannelsData[] data) {
                if (!isAdded() || data == null || data.length == 0) return;

                double sumAbsO1 = 0, sumAbsO2 = 0, sumAbsT3 = 0, sumAbsT4 = 0;
                int count = 0;

                for (SignalChannelsData packet : data) {
                    double[] samples = packet.getSamples();
                    if (samples == null || samples.length < 4) continue;

                    sumAbsO1 += Math.abs(samples[0]);
                    sumAbsO2 += Math.abs(samples[1]);
                    sumAbsT3 += Math.abs(samples[2]);
                    sumAbsT4 += Math.abs(samples[3]);
                    count++;
                }

                if (count == 0) return;

                double avgO1 = sumAbsO1 / count;
                double avgO2 = sumAbsO2 / count;
                double avgT3 = sumAbsT3 / count;
                double avgT4 = sumAbsT4 / count;

                android.util.Log.d("ConnectFragment",
                        "signal avg abs: O1=" + avgO1 +
                                " O2=" + avgO2 +
                                " T3=" + avgT3 +
                                " T4=" + avgT4);

                updateElectrodeIndicators(
                        signalToStatus(avgO1),
                        signalToStatus(avgO2),
                        signalToStatus(avgT3),
                        signalToStatus(avgT4)
                );
            }

            @Override
            public void onResistanceReceived(double o1Ohm, double o2Ohm,
                                             double t3Ohm, double t4Ohm) {
                android.util.Log.d("ConnectFragment",
                        "onResistanceReceived O1=" + o1Ohm +
                                " O2=" + o2Ohm +
                                " T3=" + t3Ohm +
                                " T4=" + t4Ohm);
            }
        });

        BrainBitConnectionStore.setManager(brainBitManager);

        SensorInfo selectedDevice = SelectedDeviceStore.getSelectedDevice();
        if (selectedDevice == null) {
            tvConnectStatus.setText("No device selected");
        } else {
            brainBitManager.connectToDevice(selectedDevice);
        }

        return root;
    }

    private ElectrodeStatus signalToStatus(double avgAbsVolts) {
        if (Double.isNaN(avgAbsVolts) || Double.isInfinite(avgAbsVolts)) {
            return ElectrodeStatus.RED;
        } else if (avgAbsVolts < 1e-7) {
            return ElectrodeStatus.RED;
        } else if (avgAbsVolts < 5e-7) {
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

    private void updateElectrodeIndicators(ElectrodeStatus o1, ElectrodeStatus o2,
                                           ElectrodeStatus t3, ElectrodeStatus t4) {
        setIndicatorStatus(indicatorO1, o1);
        setIndicatorStatus(indicatorO2, o2);
        setIndicatorStatus(indicatorT3, t3);
        setIndicatorStatus(indicatorT4, t4);

        boolean allGreen =
                o1 == ElectrodeStatus.GREEN &&
                        o2 == ElectrodeStatus.GREEN &&
                        t3 == ElectrodeStatus.GREEN &&
                        t4 == ElectrodeStatus.GREEN;

        if (btnContinueToHome != null) {
            btnContinueToHome.setVisibility(allGreen ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
