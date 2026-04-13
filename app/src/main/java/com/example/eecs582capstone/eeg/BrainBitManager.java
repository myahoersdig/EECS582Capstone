package com.example.eecs582capstone.eeg;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.neurosdk2.neuro.BrainBit2;
import com.neurosdk2.neuro.Scanner;
import com.neurosdk2.neuro.Sensor;
import com.neurosdk2.neuro.interfaces.BrainBit2ResistDataReceived;
import com.neurosdk2.neuro.interfaces.BrainBit2SignalDataReceived;
import com.neurosdk2.neuro.types.ResistRefChannelsData;
import com.neurosdk2.neuro.types.SensorCommand;
import com.neurosdk2.neuro.types.SensorFamily;
import com.neurosdk2.neuro.types.SensorInfo;
import com.neurosdk2.neuro.types.SensorState;
import com.neurosdk2.neuro.types.SignalChannelsData;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BrainBitManager {

    private static final String TAG = "BrainBitManager";

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
        void onSignalDataReceived(SignalChannelsData[] data);
        void onResistanceReceived(double o1Ohm, double o2Ohm, double t3Ohm, double t4Ohm);
    }

    private final Context context;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Scanner scanner;
    private BrainBit2 sensor;

    private volatile ConnectionState connectionState = ConnectionState.DISCONNECTED;

    private String connectedDeviceName = "device";



    private final BrainBit2SignalDataReceived signalCallback = new BrainBit2SignalDataReceived() {
        @Override
        public void onSignalDataReceived(SignalChannelsData[] data) {
            mainHandler.post(() -> {
                if (listener != null) listener.onSignalDataReceived(data);
            });
        }
    };

    private final BrainBit2ResistDataReceived resistCallback = new BrainBit2ResistDataReceived() {
        @Override
        public void onResistDataReceived(ResistRefChannelsData[] data) {
            if (data == null || data.length == 0) return;

            double[] samples = data[data.length - 1].getSamples();
            if (samples == null || samples.length < 4) return;

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onResistanceReceived(
                            samples[0], samples[1], samples[2], samples[3]
                    );
                }
            });
        }
    };

    public BrainBitManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    private final com.neurosdk2.neuro.Sensor.SensorStateChanged stateCallback =
            new com.neurosdk2.neuro.Sensor.SensorStateChanged() {
                @Override
                public void onStateChanged(SensorState state) {
                    Log.d(TAG, "stateCallback fired: " + state);

                    if (state == SensorState.StateInRange) {
                        postState(ConnectionState.CONNECTED, "Connected to " + connectedDeviceName);
                    } else {
                        postState(ConnectionState.DISCONNECTED, "Disconnected");
                    }
                }
            };

    public void connectToDevice(SensorInfo target) {
        if (target == null) {
            postState(ConnectionState.ERROR, "No device selected");
            return;
        }
        if (connectionState == ConnectionState.CONNECTING ||
                connectionState == ConnectionState.CONNECTED) {
            return;
        }

        postState(ConnectionState.CONNECTING, "Connecting to " + target.getName() + "...");

        executor.execute(() -> {
            try {
                connectedDeviceName = target.getName();

                if (scanner != null) {
                    try { scanner.close(); } catch (Exception ignored) {}
                    scanner = null;
                }

                scanner = new Scanner(new SensorFamily[]{
                        SensorFamily.SensorLEBrainBit2
                });

                sensor = (BrainBit2) scanner.createSensor(target);

                try { scanner.close(); } catch (Exception ignored) {}
                scanner = null;

                sensor.sensorStateChanged = stateCallback;

                Log.d(TAG, "createSensor returned successfully; posting CONNECTED");
                postState(ConnectionState.CONNECTED, "Connected to " + connectedDeviceName);

            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                postState(ConnectionState.ERROR,
                        e.getMessage() != null ? e.getMessage() : "Connection failed");
            }
        });
    }

    public void startSignal() {
        Log.d(TAG, "startSignal called. sensor=" + (sensor != null) + " state=" + connectionState);
        executor.execute(() -> {
            try {
                if (sensor == null || connectionState != ConnectionState.CONNECTED) return;

                sensor.signalDataReceived = signalCallback;
                sensor.execCommand(SensorCommand.StartSignal);
                Log.d(TAG, "StartSignal command sent");

                mainHandler.post(() -> {
                    if (listener != null) listener.onSignalStarted();
                });
            } catch (Exception e) {
                Log.e(TAG, "Start signal failed", e);
                postState(ConnectionState.ERROR, "Failed to start signal");
            }
        });
    }

    public void stopSignal() {
        executor.execute(() -> {
            try {
                if (sensor == null) return;
                sensor.execCommand(SensorCommand.StopSignal);
                sensor.signalDataReceived = null;
                mainHandler.post(() -> {
                    if (listener != null) listener.onSignalStopped();
                });
            } catch (Exception e) {
                Log.e(TAG, "Stop signal failed", e);
            }
        });
    }

    public void startElectrodeMonitoring() {
        Log.d(TAG, "startElectrodeMonitoring called. sensor=" + (sensor != null) + " state=" + connectionState);
        executor.execute(() -> {
            try {
                if (sensor == null || connectionState != ConnectionState.CONNECTED) return;

                sensor.resistDataReceived = resistCallback;
                sensor.execCommand(SensorCommand.StartResist);
                Log.d(TAG, "StartResist command sent");
            } catch (Exception e) {
                Log.e(TAG, "Start electrode monitoring failed", e);
            }
        });
    }

    public void stopElectrodeMonitoring() {
        executor.execute(() -> {
            try {
                if (sensor == null) return;
                sensor.execCommand(SensorCommand.StopResist);
                sensor.resistDataReceived = null;
            } catch (Exception e) {
                Log.e(TAG, "Stop electrode monitoring failed", e);
            }
        });
    }

    public void disconnect() {
        executor.execute(() -> {
            try {
                if (sensor != null) {
                    try { sensor.execCommand(SensorCommand.StopSignal); } catch (Exception ignored) {}
                    try { sensor.execCommand(SensorCommand.StopResist); } catch (Exception ignored) {}

                    sensor.signalDataReceived = null;
                    sensor.resistDataReceived = null;
                    sensor.sensorStateChanged = null;

                    try { sensor.disconnect(); } catch (Exception ignored) {}
                    try { sensor.close(); } catch (Exception ignored) {}

                    sensor = null;
                }

                if (scanner != null) {
                    try { scanner.close(); } catch (Exception ignored) {}
                    scanner = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Disconnect failed", e);
            } finally {
                postState(ConnectionState.DISCONNECTED, "Disconnected");
            }
        });
    }


    public void release() {
        disconnect();
        executor.shutdown();
    }


    private void postState(ConnectionState state, String message) {
        connectionState = state;
        mainHandler.post(() -> {
            if (listener != null) listener.onStateChanged(state, message);
        });
    }
}
