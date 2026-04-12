package com.example.eecs582capstone.eeg;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.neuromd.neurosdk.Command;
import com.neuromd.neurosdk.Device;
import com.neuromd.neurosdk.DeviceEnumerator;
import com.neuromd.neurosdk.DeviceInfo;
import com.neuromd.neurosdk.DeviceType;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class BrainBitManager {



    public enum ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    public interface Listener {
        void onStateChanged(ConnectionState state, String message);
        void onSignalStarted();
        void onSignalStopped();

        void onSignalPacketReceived(Object data);
    }

    private final Context context;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private DeviceEnumerator deviceEnumerator;
    private Device device;

    public BrainBitManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }


    public void connectToDevice(DeviceInfo target) {
        if (target == null) {
            postState(ConnectionState.ERROR, "No device selected");
            return;
        }

        postState(ConnectionState.CONNECTING, "Connecting to selected BrainBit...");

        executor.execute(() -> {
            try {
                deviceEnumerator = new DeviceEnumerator(context, DeviceType.Brainbit);
                device = deviceEnumerator.createDevice(target);
                device.connect();
                postState(ConnectionState.CONNECTED, "BrainBit connected");
            } catch (Exception e) {
                Log.e("BrainBitManager", "Connection failed", e);
                postState(ConnectionState.ERROR,
                        e.getMessage() != null ? e.getMessage() : "Connection failed");
            }
        });
    }

    public void connect() {
        postState(ConnectionState.CONNECTING, "Searching for BrainBit...");

        executor.execute(() -> {
            try {
                deviceEnumerator = new DeviceEnumerator(context, DeviceType.Brainbit);

                List<DeviceInfo> devices = deviceEnumerator.devices();
                if (devices == null || devices.isEmpty()) {
                    postState(ConnectionState.ERROR, "No BrainBit device found");
                    return;
                }

                DeviceInfo target = devices.get(0);
                device = deviceEnumerator.createDevice(target);

                device.connect();

                postState(ConnectionState.CONNECTED, "BrainBit connected");
            } catch (Exception e) {
                Log.e("BrainBitManager", "Connection failed", e);
                postState(ConnectionState.ERROR,
                        e.getMessage() != null ? e.getMessage() : "Connection failed");
            }
        });
    }

    public void startSignal() {
        executor.execute(() -> {
            try {
                if (device == null) return;
                device.execute(Command.StartSignal);
                mainHandler.post(() -> {
                    if (listener != null) listener.onSignalStarted();
                });
            } catch (Exception e) {
                Log.e("BrainBitManager", "Start signal failed", e);
                postState(ConnectionState.ERROR, "Failed to start signal");
            }
        });
    }

    public void stopSignal() {
        executor.execute(() -> {
            try {
                if (device == null) return;
                device.execute(Command.StopSignal);
                mainHandler.post(() -> {
                    if (listener != null) listener.onSignalStopped();
                });
            } catch (Exception e) {
                Log.e("BrainBitManager", "Stop signal failed", e);
            }
        });
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (device != null) {
                    device.disconnect();
                    device.close();
                    device = null;
                }

                if (deviceEnumerator != null) {
                    deviceEnumerator.close();
                    deviceEnumerator = null;
                }
            } catch (Exception e) {
                Log.e("BrainBitManager", "Disconnect failed", e);
            } finally {
                postState(ConnectionState.DISCONNECTED, "Disconnected");
            }
        });
    }

    public void release() {
        disconnect();
        executor.shutdownNow();
    }

    private void postState(ConnectionState state, String message) {
        mainHandler.post(() -> {
            if (listener != null) listener.onStateChanged(state, message);
        });
    }
}