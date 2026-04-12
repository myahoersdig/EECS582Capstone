package com.example.eecs582capstone.eeg;

import com.neuromd.neurosdk.DeviceInfo;

public final class SelectedDeviceStore {
    private static DeviceInfo selectedDevice;

    private SelectedDeviceStore() {
    }

    public static void setSelectedDevice(DeviceInfo deviceInfo) {
        selectedDevice = deviceInfo;
    }

    public static DeviceInfo getSelectedDevice() {
        return selectedDevice;
    }

    public static void clear() {
        selectedDevice = null;
    }
}