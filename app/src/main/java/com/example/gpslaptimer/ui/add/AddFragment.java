package com.example.gpslaptimer.ui.add;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;

import com.example.gpslaptimer.MainActivity;
import com.example.gpslaptimer.R;
import com.example.gpslaptimer.adapters.ConsoleLogAdapter;
import com.example.gpslaptimer.ui.map.MapViewModel;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class AddFragment extends Fragment {
    private static final String TAG = "AddFragment";
    private static final int REQUEST_CHECK_SETTINGS = 1001;

    private MapViewModel mapViewModel;

    private Button buttonStartStop;
    private RecyclerView recyclerView;

    private ConsoleLogAdapter adapter;

    private List<Location> locations = new ArrayList<>();
    private List<String> logMessages = new ArrayList<>();
    private String fileName = "";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest.Builder builder;
    private LocationCallback locationCallback;
    private ActivityResultLauncher<String[]> locationPermissionRequest;

    private boolean isTracking = false;
    private Long initialElapsedRealtimeNanos = null;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        createLocationCallback();
        createLocationRequest();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add, container, false);

        setupLocationPermissionRequest();

        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        buttonStartStop = rootView.findViewById(R.id.buttonStartStop);
        buttonStartStop.setText("Start Session");
        buttonStartStop.setOnClickListener(v -> {
            if(!isTracking) {
                checkLocationSettings();
            } else {
                stopLocationUpdates();
                releaseWakeLock();
                fileName = getUniqueFileName();
                if(saveCoordinatesToFile(locations, fileName)) {
                    mapViewModel.setFileName(fileName);
                    ((MainActivity) getActivity()).onMapFragmentRequest();
                }
            }
        });

        recyclerView = rootView.findViewById(R.id.recyclerViewConsoleLog);
        logMessages = new ArrayList<>();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ConsoleLogAdapter(logMessages);
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
        keepScreenOn(false);
    }

    private void setupLocationPermissionRequest() {
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION,false);
            if (fineLocationGranted != null && fineLocationGranted) {
                startLocationUpdates();
            } else {
                logMessage("Location permission denied");
            }
        });
    }

    private void requestLocationPermission() {
        locationPermissionRequest.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(0)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(1)
                .setIntervalMillis(0)
                .setMinUpdateIntervalMillis(0)
                .build();
        builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    locations.add(location);
                    if(initialElapsedRealtimeNanos == null) {
                        initialElapsedRealtimeNanos = location.getElapsedRealtimeNanos();
                    }
                    double elapsedTime = (location.getElapsedRealtimeNanos() - initialElapsedRealtimeNanos) / 1000000000.0;
                    logMessage(location.getLatitude() + "," + location.getLongitude() + "," + location.getSpeed() + "," + String.format("%.3f", elapsedTime));
                }
            }
        };
    }

    private void checkLocationSettings() {
        SettingsClient client = LocationServices.getSettingsClient(requireActivity());
        Task<LocationSettingsResponse> task = client.checkLocationSettings(builder.build());

        task.addOnSuccessListener(requireActivity(), new OnSuccessListener<LocationSettingsResponse>() {

            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                startLocationUpdates();
                acquireWakeLock();
            }
        });

        task.addOnFailureListener(requireActivity(), new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e instanceof ResolvableApiException) {
                    // Location settings not satisfied
                    try {
                        // Show dialog
                        ResolvableApiException resolvable = (ResolvableApiException) e;
                        resolvable.startResolutionForResult(requireActivity(), REQUEST_CHECK_SETTINGS);
                    } catch (IntentSender.SendIntentException sendEx) {
                        Log.e(TAG, "Error showing location settings resolution dialog", sendEx);
                    }
                }
            }
        });
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermission();
            return;
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        isTracking = true;
        buttonStartStop.setText("Stop Session");
        keepScreenOn(true);
        logMessage("Location updates started");
    }

    private void stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        isTracking = false;
        buttonStartStop.setText("Start");
        logMessage("Location updates stopped");
        keepScreenOn(false);
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
    private void keepScreenOn(boolean on) {
        if (getActivity() != null) {
            if (on) {
                getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        }
    }



    private void logMessage(String message) {
        adapter.addMessage(message);
        recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        Log.d(TAG, message);
    }

    private boolean saveCoordinatesToFile(List<Location> coordinates, String fileName) {
        File directory = getContext().getExternalFilesDir(null);
        File file = new File(directory, fileName);
        int count = 0;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Location location : coordinates) {
                double elapsedTime = (location.getElapsedRealtimeNanos() - initialElapsedRealtimeNanos) / 1000000000.0;
                writer.write(location.getLatitude() + "," + location.getLongitude() + "," + location.getSpeed() + "," + String.format("%.3f", elapsedTime));
                writer.newLine();
                count++;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error writing coordinates to file", e);
            return false;
        }
        logMessage("Wrote " + count + " lines to " + fileName);
        return true;
    }

    private String getUniqueFileName() {
        String baseName = "Unknown date";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            LocalDate today = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");
            baseName = today.format(formatter);
        }

        if (getContext() == null) return baseName;
        File directory = getContext().getExternalFilesDir(null);
        if (directory == null) return baseName;

        File file = new File(directory, baseName);
        int index = 1;
        while (file.exists()) {
            String newName = baseName + "(" + index + ")";
            logMessage("Filename is now " + newName);
            file = new File(directory, newName);
            index++;
        }
        return file.getName();
    }

}