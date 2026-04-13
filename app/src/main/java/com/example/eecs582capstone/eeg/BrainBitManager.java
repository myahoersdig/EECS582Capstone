package com.example.eecs582capstone.eeg;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.neurosdk2.neuro.BrainBit2;
import com.neurosdk2.neuro.Scanner;
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

    public BrainBitManager(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
    }

    public void connectToDevice(SensorInfo target) {
        if (target == null) {
            postState(ConnectionState.ERROR, "No device selected");
            return;
        }

        postState(ConnectionState.CONNECTING, "Connecting to " + target.getName() + "...");

        executor.execute(() -> {
            try {
                scanner = new Scanner(new SensorFamily[]{
                        SensorFamily.SensorLEBrainBit,
                        SensorFamily.SensorLEBrainBitBlack,
                        SensorFamily.SensorLEBrainBit2,
                        SensorFamily.SensorLEBrainBitPro,
                        SensorFamily.SensorLEBrainBitFlex
                });
                sensor = (BrainBit2) scanner.createSensor(target);

                sensor.sensorStateChanged = state -> {
                    Log.d(TAG, "sensorStateChanged: " + state);
                    if (state == SensorState.StateInRange) {
                        postState(ConnectionState.CONNECTED, "Connected to " + target.getName());
                    } else {
                        postState(ConnectionState.DISCONNECTED, "Disconnected");
                    }
                };

                sensor.connect();

            } catch (Exception e) {
                Log.e(TAG, "Connection failed", e);
                postState(ConnectionState.ERROR,
                        e.getMessage() != null ? e.getMessage() : "Connection failed");
            }
        });
    }

    public void startSignal() {
        executor.execute(() -> {
            try {
                if (sensor == null) return;

                sensor.signalDataReceived = new BrainBit2SignalDataReceived() {
                    @Override
                    public void onSignalDataReceived(SignalChannelsData[] data) {
                        mainHandler.post(() -> {
                            if (listener != null) listener.onSignalDataReceived(data);
                        });
                    }
                };

                sensor.execCommand(SensorCommand.StartSignal);
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
        executor.execute(() -> {
            try {
                if (sensor == null) return;

                sensor.resistDataReceived = new BrainBit2ResistDataReceived() {
                    @Override
                    public void onResistDataReceived(ResistRefChannelsData[] data) {
                        if (data == null || data.length == 0) return;
                        // Take the most recent packet; samples[] = [O1, O2, T3, T4]
                        double[] samples = data[data.length - 1].getSamples();
                        if (samples == null || samples.length < 4) return;
                        mainHandler.post(() -> {
                            if (listener != null) {
                                listener.onResistanceReceived(
                                        samples[0], samples[1], samples[2], samples[3]);
                            }
                        });
                    }
                };

                sensor.execCommand(SensorCommand.StartResist);
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
                    sensor.signalDataReceived = null;
                    sensor.resistDataReceived = null;
                    sensor.sensorStateChanged = null;
                    sensor.disconnect();
                    sensor.close();
                    sensor = null;
                }
                if (scanner != null) {
                    scanner.close();
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
        executor.shutdownNow();
    }

    private void postState(ConnectionState state, String message) {
        mainHandler.post(() -> {
            if (listener != null) listener.onStateChanged(state, message);
        });
    }
}
