package com.example.eecs582capstone.eeg;

import com.neurosdk2.neuro.types.SensorInfo;

public final class SelectedDeviceStore {
    private static SensorInfo selectedDevice;

    private SelectedDeviceStore() {
    }

    public static void setSelectedDevice(SensorInfo sensorInfo) {
        selectedDevice = sensorInfo;
    }

    public static SensorInfo getSelectedDevice() {
        return selectedDevice;
    }

    public static void clear() {
        selectedDevice = null;
    }
}
