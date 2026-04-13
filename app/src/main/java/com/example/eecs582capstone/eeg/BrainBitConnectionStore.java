package com.example.eecs582capstone.eeg;

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