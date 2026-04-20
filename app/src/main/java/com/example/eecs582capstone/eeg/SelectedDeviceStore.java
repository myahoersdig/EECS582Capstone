package com.example.eecs582capstone.eeg;

import com.neurosdk2.neuro.types.SensorInfo;

/*
Filename: BrainBitManager.java
Author(s): Riley England
Created: 04-11-2026
Last Modified: 04-19-2026
Overview and Purpose: Stores the currently selected EEG device so it can be
accessed across different parts of the application.
Notes: This class is different from the BrainBitConnectionStore as it keeps the specific device as a static variable, 
instead of the connection.
*/

/*
SelectedDeviceStore class: Utility class that maintains a shared static reference to the
currently selected EEG device (SensorInfo) for use throughout the application.
*/
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
