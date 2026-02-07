package com.example.gpslaptimer.wear;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WearLocationService extends Service {

    private static final String TAG = "WearLocationService";
    public static final String ACTION_START = "ACTION_START";
    public static final String ACTION_STOP = "ACTION_STOP";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "WearLocationTrackingChannel";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private BufferedWriter csvWriter;
    private String currentFileName;
    private Long initialElapsedRealtimeNanos;
    private boolean isTracking = false;

    private static boolean sIsTracking = false;
    private static long sTrackingStartTimeMillis = 0;

    public static boolean isCurrentlyTracking() {
        return sIsTracking;
    }

    public static long getTrackingStartTimeMillis() {
        return sTrackingStartTimeMillis;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                start();
            } else if (ACTION_STOP.equals(action)) {
                stop();
            }
        }
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        if (isTracking) {
            Log.w(TAG, "Already tracking, ignoring duplicate start");
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted, stopping");
            stopSelf();
            return;
        }

        initialElapsedRealtimeNanos = null;
        openCsvWriter();
        createLocationCallback();
        createLocationRequest();

        fusedLocationProviderClient.requestLocationUpdates(
                locationRequest, locationCallback, Looper.getMainLooper());

        isTracking = true;
        sIsTracking = true;
        sTrackingStartTimeMillis = System.currentTimeMillis();
        startForeground(NOTIFICATION_ID, createNotification());
        Log.d(TAG, "Tracking started, writing to: " + currentFileName);
    }

    private void stop() {
        if (!isTracking) {
            return;
        }

        isTracking = false;
        sIsTracking = false;
        sTrackingStartTimeMillis = 0;
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        closeCsvWriter();

        Intent finishedIntent = new Intent("TRACKING_FINISHED");
        finishedIntent.setPackage(getPackageName());
        finishedIntent.putExtra("fileName", currentFileName);
        sendBroadcast(finishedIntent);
        Log.d(TAG, "Tracking finished, file: " + currentFileName);

        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (csvWriter != null) {
            Log.w(TAG, "onDestroy: CSV writer still open, closing");
            closeCsvWriter();
        }
    }

    private void broadcastLocation(Location location) {
        Intent intent = new Intent("LOCATION_UPDATED");
        intent.setPackage(getPackageName());
        intent.putExtra("location", location);
        sendBroadcast(intent);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (initialElapsedRealtimeNanos == null) {
                        initialElapsedRealtimeNanos = location.getElapsedRealtimeNanos();
                    }
                    writeCsvLine(location);
                    broadcastLocation(location);
                }
            }
        };
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(0)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(1)
                .setIntervalMillis(0)
                .setMinUpdateIntervalMillis(0)
                .build();
    }

    private void writeCsvLine(Location location) {
        if (csvWriter == null) {
            return;
        }
        try {
            double elapsedTimeSeconds =
                    (location.getElapsedRealtimeNanos() - initialElapsedRealtimeNanos) / 1_000_000_000.0;
            csvWriter.write(location.getLatitude() + ","
                    + location.getLongitude() + ","
                    + location.getSpeed() + ","
                    + String.format("%.3f", elapsedTimeSeconds));
            csvWriter.newLine();
            csvWriter.flush();
        } catch (IOException e) {
            Log.e(TAG, "Error writing CSV line", e);
        }
    }

    private void openCsvWriter() {
        File directory = getExternalFilesDir(null);
        if (directory == null) {
            Log.e(TAG, "External files directory is null, CSV will not be written");
            return;
        }
        currentFileName = getUniqueFileName(directory);
        File file = new File(directory, currentFileName);
        try {
            csvWriter = new BufferedWriter(new FileWriter(file));
        } catch (IOException e) {
            Log.e(TAG, "Error opening CSV writer", e);
            csvWriter = null;
        }
    }

    private void closeCsvWriter() {
        if (csvWriter != null) {
            try {
                csvWriter.flush();
                csvWriter.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing CSV writer", e);
            }
            csvWriter = null;
        }
    }

    private String getUniqueFileName(File directory) {
        String baseName = "Unknown date";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            baseName = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        }
        File file = new File(directory, baseName);
        int index = 1;
        while (file.exists()) {
            String newName = baseName + "(" + index + ")";
            file = new File(directory, newName);
            index++;
        }
        return file.getName();
    }

    private Notification createNotification() {
        createNotificationChannel();
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Lap Timer")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.ic_gps_tracking)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
}
