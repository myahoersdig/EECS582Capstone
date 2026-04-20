/*
Filename: BrainBitConnectionStore.java
Author(s): Riley England
Created: 04-11-2026
Last Modified: 04-19-2026
Overview and Purpose: Stores a shared reference to the active BrainBitManager instance so that other 
parts of the application can access the current EEG device connection state without passing the manager object between components directly.
Notes: n/a
*/

package com.example.eecs582capstone.eeg;

// BrainBitConnectionStore class: Utility class that maintains a shared static reference to the BrainBitManager so that the app can access and manage the active connection across components.

public class BrainBitConnectionStore {
    private static BrainBitManager manager;

    public static void setManager(BrainBitManager brainBitManager) {
        manager = brainBitManager;
    }

    public static BrainBitManager getManager() {
        return manager;
    }

    public static boolean hasManager() {
        return manager != null;
    }

    public static void clear() {
        manager = null;
    }
}
