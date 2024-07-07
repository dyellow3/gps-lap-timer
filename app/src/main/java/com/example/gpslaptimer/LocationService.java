package com.example.gpslaptimer;

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

import com.example.gpslaptimer.ui.add.AddFragment;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.Priority;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String ACTION_START = "ACTION_START";
    private static final String ACTION_STOP = "ACTION_STOP";
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "LocationTrackingChannel";

    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private List<Location> locations = new ArrayList<>();
    private Long initialElapsedRealtimeNanos = null;

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
                locations = new ArrayList<>();
                start();
            } else if (ACTION_STOP.equals(action)) {
                stop();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void start() {
        createLocationCallback();
        createLocationRequest();

        // Start location updates
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        startForeground(NOTIFICATION_ID, createNotification());
    }

    private void stop() {
        // Stop location updates
        fusedLocationProviderClient.removeLocationUpdates(locationCallback);

        stopForeground(true);
        stopSelf();
    }

    private void broadcastLocation(Location location) {
        Intent intent = new Intent("LOCATION_UPDATED");
        intent.setPackage(getPackageName());
        intent.putExtra("location", location);
        Log.d(TAG, "Broadcasting location: " + location.toString());
        sendBroadcast(intent);
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    locations.add(location);
                    if(initialElapsedRealtimeNanos == null) {
                        initialElapsedRealtimeNanos = location.getElapsedRealtimeNanos();
                    }
                    broadcastLocation(location);
                }
            }
        };
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest.Builder(0)
                .setGranularity(Granularity.GRANULARITY_FINE)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateDistanceMeters(1)
                .setIntervalMillis(0)
                .setMinUpdateIntervalMillis(0)
                .build();
    }

    private Notification createNotification() {
        createNotificationChannel();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("GPS Lap Timer")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.baseline_add_24)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Location Tracking Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}