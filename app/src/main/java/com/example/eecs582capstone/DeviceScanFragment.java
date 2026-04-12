package com.example.eecs582capstone;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.util.Log;

import com.neuromd.neurosdk.DeviceEnumerator;
import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceType;

import java.util.ArrayList;
import java.util.List;
import com.example.eecs582capstone.eeg.SelectedDeviceStore;

public class DeviceScanFragment extends Fragment {

    private androidx.activity.result.ActivityResultLauncher<String[]> permissionLauncher;

    private TextView tvScanStatus;
    private Button btnScanDevices;
    private ListView lvDevices;

    private ArrayAdapter<String> adapter;
    private final List<DeviceInfo> foundDevices = new ArrayList<>();
    private final List<String> deviceNames = new ArrayList<>();

    private DeviceEnumerator deviceEnumerator;

    public DeviceScanFragment() {}

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

        btnScanDevices.setOnClickListener(v -> startScan());

        lvDevices.setOnItemClickListener((parent, view, position, id) -> {
            DeviceInfo selectedDevice = foundDevices.get(position);

            SelectedDeviceStore.setSelectedDevice(selectedDevice);

            Bundle args = new Bundle();
            args.putString("device_name", selectedDevice.toString());

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

    private void startScan() {
        if (!hasBluetoothPermissions()) {
            tvScanStatus.setText("Requesting Bluetooth permissions...");
            requestBluetoothPermissions();
            return;
        }

        tvScanStatus.setText("Scanning for BrainBit devices...");
        deviceNames.clear();
        foundDevices.clear();
        adapter.notifyDataSetChanged();

        try {
            deviceEnumerator = new DeviceEnumerator(requireContext(), DeviceType.Brainbit);

            // simple first pass: wait a moment, then read current devices list
            lvDevices.postDelayed(() -> {
                if (!isAdded()) return;

                List<DeviceInfo> devices = deviceEnumerator.devices();

                if (devices == null || devices.isEmpty()) {
                    tvScanStatus.setText("No BrainBit devices found");
                    return;
                }

                foundDevices.addAll(devices);
                for (DeviceInfo device : devices) {
                    deviceNames.add(device.toString());
                }
                adapter.notifyDataSetChanged();
                tvScanStatus.setText("Select a device");
            }, 3000);

        } catch (Exception e) {
            tvScanStatus.setText("Scan failed");
            Toast.makeText(requireContext(), "Scan failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private boolean hasBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (deviceEnumerator != null) {
            try {
                deviceEnumerator.close();
            } catch (InterruptedException e) {
                Log.e("DeviceScanFragment", "Interrupted while closing device enumerator", e);
                Thread.currentThread().interrupt();
            }
            deviceEnumerator = null;
        }
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            });
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            });
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        permissionLauncher = registerForActivityResult(
                new androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean allGranted = true;

                    for (Boolean granted : result.values()) {
                        if (granted == null || !granted) {
                            allGranted = false;
                            break;
                        }
                    }

                    if (allGranted) {
                        startScan();
                    } else {
                        if (isAdded()) {
                            tvScanStatus.setText("Bluetooth permissions denied");
                            Toast.makeText(requireContext(), "Bluetooth permissions are required", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
    }
}