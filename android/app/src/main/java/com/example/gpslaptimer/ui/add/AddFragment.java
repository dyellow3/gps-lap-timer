package com.example.gpslaptimer.ui.add;

import androidx.lifecycle.ViewModelProvider;

import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.gpslaptimer.MainActivity;
import com.example.gpslaptimer.R;
import com.example.gpslaptimer.adapters.ConsoleLogAdapter;
import com.example.gpslaptimer.ui.connect.ConnectViewModel;
import com.example.gpslaptimer.utils.LapDetection;
import com.example.gpslaptimer.models.LocationData;
import com.example.gpslaptimer.ui.map.MapViewModel;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddFragment extends Fragment {
    private MapViewModel mapViewModel;
    private static final String TAG = "AddFragment";
    private ConnectViewModel connectViewModel;
    Button buttonStartStop;
    private boolean flag = false;
    private List<LocationData> coordinates = new ArrayList<>();
    private Thread readThread;
    String fileName = "";
    RecyclerView recyclerView;
    private ConsoleLogAdapter adapter;
    private List<String> logMessages;
    private PowerManager.WakeLock wakeLock;
    private LocationData prev;

    public static AddFragment newInstance() {
        return new AddFragment();
    }

    private void acquireWakeLock() {
        PowerManager powerManager = (PowerManager) getContext().getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyApp::WakeLock");
        wakeLock.acquire();
    }
    private void releaseWakeLock() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add, container, false);

        connectViewModel = new ViewModelProvider(requireActivity()).get(ConnectViewModel.class);
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        buttonStartStop = rootView.findViewById(R.id.buttonStartStop);

        logMessages = new ArrayList<>();
        recyclerView = rootView.findViewById(R.id.recyclerViewConsoleLog);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConsoleLogAdapter(logMessages);
        recyclerView.setAdapter(adapter);

        connectViewModel.getConnectionStatus().observe(getViewLifecycleOwner(), isConnected -> {
            if (isConnected != null) {
                buttonStartStop.setEnabled(isConnected);
                Log.d(TAG, "Connection Status: " + isConnected);
                if(isConnected) {
                    //startReadingMessages();
                    connectViewModel.getReceivedMessage().observe(getViewLifecycleOwner(), message -> {
                        // Process the received message
                        if (!message.isEmpty()) {
                            Log.d(TAG, "Received Message: " + message);
                            logMessage("Received Message: " + message);
                        }

                        if (message.contains("Starting GPS")) {
                            flag = true;
                            acquireWakeLock();
                            coordinates.clear();
                            prev = null;
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                LocalDate today = LocalDate.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
                                fileName = getUniqueFileName(today.format(formatter));
                            } else {
                                fileName = "Unknown date";
                            }
                        } else if (message.contains("Stopping GPS")) {
                            flag = false;
                            releaseWakeLock();
                            fileName = getUniqueFileName(fileName);
                            if(saveCoordinatesToFile(coordinates, fileName)) {
                                mapViewModel.setFileName(fileName);
                                ((MainActivity) getActivity()).onMapFragmentRequest();
                            }
                        } else if (flag) {
                            String[] parts = message.split(",");
                            if(parts.length == 3) {
                                double latitude = Double.parseDouble(parts[0].trim());
                                double longitude = Double.parseDouble(parts[1].trim());
                                double speed = Double.parseDouble(parts[2].trim());
                                LocationData curr = new LocationData(new LatLng(latitude, longitude), speed, 1);

                                if(prev != null) {
                                    double distance = LapDetection.calculateDistance(curr.getCoordinate(), prev.getCoordinate());
                                    if(distance > 1) {
                                        coordinates.add(curr);
                                        prev = curr;
                                    } else {
                                        prev.setWeight(prev.getWeight() + 1);
                                    }
                                } else {
                                    coordinates.add(curr);
                                    prev = curr;
                                }
                            }
                        }
                    });
                } else {
                    stopReadingMessages();
                }
            }
        });

        buttonStartStop.setOnClickListener(v -> {
            BluetoothSocket bluetoothSocket = connectViewModel.getBluetoothSocket().getValue();
            if(bluetoothSocket != null && bluetoothSocket.isConnected()) {
                OutputStream outputStream = null;
                try {
                    outputStream = bluetoothSocket.getOutputStream();
                    if(!flag) {
                        outputStream.write("start;".getBytes());
                        logMessage("Sent 'start; command to the Bluetooth device");
                        Log.d(TAG, "Sent 'start; command to the Bluetooth device");
                    } else {
                        outputStream.write("stop;".getBytes());
                        logMessage("Sent 'stop; command to the Bluetooth device");
                        Log.d(TAG, "Sent 'stop; command to the Bluetooth device");
                    }
                    outputStream.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        });


        return rootView;
    }

    private void logMessage(String message) {
        adapter.addMessage(message);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    private void stopReadingMessages() {
        if(readThread != null && readThread.isAlive()) {
            readThread.interrupt();
        }
    }

    private boolean saveCoordinatesToFile(List<LocationData> coordinates, String fileName) {
        File directory = getContext().getExternalFilesDir(null);
        File file = new File(directory, fileName);
        int count = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (LocationData locationData : coordinates) {
                writer.write(locationData.getCoordinate().latitude + "," + locationData.getCoordinate().longitude + "," + locationData.getSpeed());
                writer.newLine();
                count++;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing coordinates to file", e);
            return false;
        }
        logMessage("Wrote " + count + " lines to " + fileName);
        Log.d(TAG, "Wrote " + count + " lines to " + fileName);
        return true;
    }

    private String getUniqueFileName(String baseName) {
        if (getContext() == null) return baseName;
        File directory = getContext().getExternalFilesDir(null);
        if (directory == null) return baseName;

        File file = new File(directory, baseName);
        int index = 1;
        while (file.exists()) {
            String newName = baseName + "(" + index + ")";
            Log.d(TAG, "Filename is now " + newName);
            logMessage("Filename is now " + newName);
            file = new File(directory, newName);
            index++;
        }
        return file.getName();
    }
}