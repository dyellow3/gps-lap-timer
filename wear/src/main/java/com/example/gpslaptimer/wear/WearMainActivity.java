package com.example.gpslaptimer.wear;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.gpslaptimer.wear.databinding.ActivityWearMainBinding;

import java.util.Locale;

public class WearMainActivity extends AppCompatActivity {

    private ActivityWearMainBinding binding;

    private ActivityResultLauncher<String> locationPermissionLauncher;

    private boolean isTracking = false;
    private int pointCount = 0;

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateTimerDisplay();
            timerHandler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("LOCATION_UPDATED".equals(intent.getAction())) {
                pointCount++;
                binding.textStatus.setText(
                        getString(R.string.status_gps_points, pointCount));
            }
        }
    };

    private final BroadcastReceiver finishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("TRACKING_FINISHED".equals(intent.getAction())) {
                resetToIdle();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityWearMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupPermissionLauncher();

        binding.buttonStartStop.setOnClickListener(v -> {
            if (!isTracking) {
                onStartClicked();
            } else {
                onStopClicked();
            }
        });

        // Restore state if service is already tracking (e.g. Activity recreation)
        if (WearLocationService.isCurrentlyTracking()) {
            isTracking = true;
            pointCount = 0; // We don't know the count, but timer is accurate
            binding.buttonStartStop.setText(R.string.button_stop);
            binding.textStatus.setText(R.string.status_tracking);
            startTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter locationFilter = new IntentFilter("LOCATION_UPDATED");
        IntentFilter finishedFilter = new IntentFilter("TRACKING_FINISHED");

        ContextCompat.registerReceiver(this, locationReceiver, locationFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
        ContextCompat.registerReceiver(this, finishedReceiver, finishedFilter,
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(locationReceiver);
        unregisterReceiver(finishedReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacks(timerRunnable);
    }

    private void setupPermissionLauncher() {
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                granted -> {
                    if (granted) {
                        startTracking();
                    } else {
                        Toast.makeText(this, R.string.permission_denied,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void onStartClicked() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
            return;
        }
        startTracking();
    }

    private void onStopClicked() {
        Intent intent = new Intent(this, WearLocationService.class);
        intent.setAction(WearLocationService.ACTION_STOP);
        startService(intent);
        resetToIdle();
    }

    private void startTracking() {
        if (isTracking) {
            return;
        }

        Intent intent = new Intent(this, WearLocationService.class);
        intent.setAction(WearLocationService.ACTION_START);
        startForegroundService(intent);

        isTracking = true;
        pointCount = 0;
        binding.buttonStartStop.setText(R.string.button_stop);
        binding.textStatus.setText(R.string.status_tracking);
        startTimer();
    }

    private void resetToIdle() {
        isTracking = false;
        pointCount = 0;
        timerHandler.removeCallbacks(timerRunnable);
        binding.textTimer.setText(R.string.timer_default);
        binding.textStatus.setText(R.string.status_ready);
        binding.buttonStartStop.setText(R.string.button_start);
    }

    private void startTimer() {
        timerHandler.removeCallbacks(timerRunnable);
        timerHandler.post(timerRunnable);
    }

    private void updateTimerDisplay() {
        long startTime = WearLocationService.getTrackingStartTimeMillis();
        if (startTime == 0) {
            return;
        }

        long elapsedMillis = System.currentTimeMillis() - startTime;
        long totalSeconds = elapsedMillis / 1000;
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        String formatted;
        if (hours > 0) {
            formatted = String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        } else {
            formatted = String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
        binding.textTimer.setText(formatted);
    }
}
