package com.example.eecs582capstone;

/*
Filename: DeviceScanFragment.java
Author(s): Riley England
Created: 04-12-2026
Last Modified: 04-19-2026
Overview and Purpose: Handles scanning for nearby EEG devices using the NeuroSDK,
displaying discovered devices to the user, and allowing selection of a device to
initiate the connection process.
Notes: This fragment manages Bluetooth and location permission requests, performs
asynchronous device scanning using the NeuroSDK Scanner, and updates the UI in real-time
as devices are discovered. It maintains a list of detected devices, prevents duplicates,
and transitions to the connection screen upon user selection. The scanning process is
time-limited and includes a countdown indicator for user feedback.
Pipeline is: Permissions -> Scan -> Select Device -> ConnectFragment
*/

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.eecs582capstone.eeg.SelectedDeviceStore;
import com.neurosdk2.neuro.Scanner;
import com.neurosdk2.neuro.types.SensorFamily;
import com.neurosdk2.neuro.types.SensorInfo;

import java.util.ArrayList;
import java.util.List;

/*
DeviceScanFragment class: A UI controller responsible for scanning for available EEG devices,
displaying them in a list, and allowing the user to select a device for connection.
*/
public class DeviceScanFragment extends Fragment {

    private static final String TAG = "DeviceScanFragment";
    private static final int SCAN_DURATION_MS = 20_000;
    private static final int POLL_INTERVAL_MS = 2_000;

    private TextView tvScanStatus;
    private Button btnScanDevices;
    private ListView lvDevices;

    private ArrayAdapter<String> adapter;
    private final List<SensorInfo> foundSensors = new ArrayList<>();
    private final List<String> deviceNames = new ArrayList<>();

    private Scanner scanner;
    private final Handler scanHandler = new Handler(Looper.getMainLooper());
    private Runnable stopScanRunnable;
    private Runnable countdownRunnable;
    private Runnable pollRunnable;
    private boolean isScanning = false;
    private int remainingSeconds;

    private ActivityResultLauncher<String[]> permissionLauncher;

    public DeviceScanFragment() {}

    private final Scanner.ScannerCallback scanCallback = (sc, sensors) -> {
        if (!isAdded() || sensors == null) return;

        scanHandler.post(() -> {
            boolean changed = false;
            for (SensorInfo s : sensors) {
                boolean exists = false;
                for (SensorInfo existing : foundSensors) {
                    if (existing.getAddress().equals(s.getAddress())) {
                        exists = true;
                        break;
                    }
                }
                if (!exists) {
                    foundSensors.add(s);
                    String name = s.getName();
                    if (name == null || name.isEmpty()) name = "Unknown";
                    deviceNames.add(name + "  [" + s.getAddress() + "]");
                    changed = true;
                }
            }
            if (changed) adapter.notifyDataSetChanged();
        });
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;
                    for (Boolean granted : result.values()) {
                        if (granted == null || !granted) { allGranted = false; break; }
                    }
                    if (allGranted) {
                        startScan();
                    } else if (isAdded()) {
                        tvScanStatus.setText("Permissions denied — cannot scan");
                        Toast.makeText(requireContext(),
                                "Bluetooth and Location permissions are required",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_device_scan, container, false);

        tvScanStatus = root.findViewById(R.id.tvScanStatus);
        btnScanDevices = root.findViewById(R.id.btnScanDevices);
        lvDevices = root.findViewById(R.id.lvDevices);

        adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1,
                deviceNames);
        lvDevices.setAdapter(adapter);

        btnScanDevices.setOnClickListener(v -> {
            if (isScanning) {
                stopScan(false);
            } else {
                checkPermissionsAndScan();
            }
        });

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            SensorInfo selected = foundSensors.get(position);
            SelectedDeviceStore.setSelectedDevice(selected);
            stopScan(true);
            Bundle args = new Bundle();
            String name = selected.getName();
            if (name == null || name.isEmpty()) name = selected.getAddress();
            args.putString("device_name", name);

            ConnectFragment connectFragment = new ConnectFragment();
            connectFragment.setArguments(args);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.flFragment, connectFragment)
                    .addToBackStack(null)
                    .commit();
        });

        return root;
    }

    private void checkPermissionsAndScan() {
        if (hasRequiredPermissions()) {
            startScan();
        } else {
            tvScanStatus.setText("Requesting permissions...");
            permissionLauncher.launch(requiredPermissions());
        }
    }

    private boolean hasRequiredPermissions() {
        for (String perm : requiredPermissions()) {
            if (ContextCompat.checkSelfPermission(requireContext(), perm)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] requiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        } else {
            return new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            };
        }
    }

    private void startScan() {
        if (isScanning) return;

        isScanning = true;
        btnScanDevices.setText("Stop");
        deviceNames.clear();
        foundSensors.clear();
        adapter.notifyDataSetChanged();

        remainingSeconds = SCAN_DURATION_MS / 1000;
        tickCountdown();

        try {
            closeScanner();
            Log.d(TAG, "Creating Scanner(SensorLEBrainBit)...");
            scanner = new Scanner(new SensorFamily[]{
                    SensorFamily.SensorLEBrainBit2,
            });

            scanner.sensorsChanged = scanCallback;

            scanner.start();
            Log.d(TAG, "Scanner started");

            // schedulePolls();

            stopScanRunnable = () -> { if (isAdded()) stopScan(false); };
            scanHandler.postDelayed(stopScanRunnable, SCAN_DURATION_MS);

        } catch (Exception e) {
            isScanning = false;
            btnScanDevices.setText("Scan");
            String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            tvScanStatus.setText("Scan failed: " + msg);
            Log.e(TAG, "Scanner creation failed", e);
        }
    }

    private void stopScan(boolean navigatedAway) {
        Log.d(TAG, "stopScan called. isScanning=" + isScanning + ", navigatedAway=" + navigatedAway);
        if (!isScanning) return;

        isScanning = false;

        if (stopScanRunnable != null) {
            scanHandler.removeCallbacks(stopScanRunnable);
            stopScanRunnable = null;
        }
        if (countdownRunnable != null) {
            scanHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }
        if (pollRunnable != null) {
            scanHandler.removeCallbacks(pollRunnable);
            pollRunnable = null;
        }

        try {
            if (scanner != null) {
                scanner.stop();
                scanner.sensorsChanged = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "scanner.stop() failed", e);
        }

        if (!navigatedAway) {
            btnScanDevices.setText("Scan Again");
            if (foundSensors.isEmpty()) {
                tvScanStatus.setText("No devices found — make sure the headset is on and tap Scan Again");
            } else {
                tvScanStatus.setText("Found " + foundSensors.size() + " device(s) — select one below");
            }
        }
    }

    private void refreshSensorList() {
        if (scanner == null || !isAdded()) return;

        List<SensorInfo> current = scanner.getSensors();
        Log.d(TAG, "getSensors() = " + (current == null ? "null" : current.size()));
        if (current == null) return;

        boolean changed = false;
        for (SensorInfo s : current) {
            boolean exists = false;
            for (SensorInfo existing : foundSensors) {
                if (existing.getAddress().equals(s.getAddress())) { exists = true; break; }
            }
            if (!exists) {
                foundSensors.add(s);
                String name = s.getName();
                if (name == null || name.isEmpty()) name = "Unknown";
                deviceNames.add(name + "  [" + s.getAddress() + "]");
                changed = true;
                Log.d(TAG, "Found: " + name + " @ " + s.getAddress());
            }
        }

        if (changed) adapter.notifyDataSetChanged();
    }

    private void schedulePolls() {
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isScanning || !isAdded()) return;
                refreshSensorList();
                scanHandler.postDelayed(this, POLL_INTERVAL_MS);
            }
        };
        scanHandler.postDelayed(pollRunnable, POLL_INTERVAL_MS);
    }

    private void tickCountdown() {
        if (!isScanning || !isAdded()) return;
        if (foundSensors.isEmpty()) {
            tvScanStatus.setText("Scanning... " + remainingSeconds + "s");
        }
        if (remainingSeconds > 0) {
            remainingSeconds--;
            countdownRunnable = this::tickCountdown;
            scanHandler.postDelayed(countdownRunnable, 1000);
        }
    }

    private void closeScanner() {
        if (scanner != null) {
            try {
                scanner.sensorsChanged = null;
                scanner.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing scanner", e);
            }
            scanner = null;
        }
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView called. isScanning=" + isScanning);
        super.onDestroyView();
        if (isScanning) {
            stopScan(true);
        }
        closeScanner();
    }
}
