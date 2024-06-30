package com.example.gpslaptimer.ui.map;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gpslaptimer.utils.LapDetection;
import com.example.gpslaptimer.utils.MapDrawing;
import com.example.gpslaptimer.R;
import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.LocationData;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.Polyline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private static final String TAG = "MapFragment";
    private SupportMapFragment mapFragment;
    private List<LocationData> locationData = new ArrayList<>();
    private GoogleMap googleMap;
    private List<Polyline> polylines = new ArrayList<>();
    private List<Marker> markers = new ArrayList<>();
    private int currentLapIndex = 0;
    private List<Lap> laps;
    private String fileName = "";
    private MapViewModel mapViewModel;
    private Button nextButton;
    private TextView textViewLapTime;
    private TextView textViewLaps;
    private TextView textCurrentLap;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        nextButton = rootView.findViewById(R.id.buttonNext);
        textViewLapTime = rootView.findViewById(R.id.textViewLapTime);
        textViewLaps = rootView.findViewById(R.id.textViewLaps);
        textCurrentLap = rootView.findViewById(R.id.textCurrentLap);

        mapViewModel.getFileName().observe(getViewLifecycleOwner(), newFileName -> {
            fileName = newFileName;
            readAndStoreCoordinates(fileName);
        });

        mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.maps);
        if (mapFragment == null) {
            mapFragment = SupportMapFragment.newInstance();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.maps, mapFragment)
                    .commit();
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (!locationData.isEmpty()) {
            requireActivity().runOnUiThread((this::processLocationData));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationData.get((0)).getCoordinate().latitude, locationData.get((0)).getCoordinate().longitude), 15));
        } else {
            showMessage("No location data detected");
            Log.d(TAG, "No location data detected");
        }
    }

    private void readAndStoreCoordinates(String fileName) {
        locationData.clear();
        File directory = getContext().getExternalFilesDir(null);
        File file = new File(directory, fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            LocationData prev = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    double speed = parts.length == 3 ? Double.parseDouble(parts[2]) : 0;
                    LocationData curr = new LocationData(new LatLng(latitude, longitude), speed, 1);

                    if(prev != null) {
                        double distance = LapDetection.calculateDistance(curr.getCoordinate(), prev.getCoordinate());
                        if(distance > 1) {
                            this.locationData.add(curr);
                            prev = curr;
                        } else {
                            prev.setWeight(prev.getWeight() + 1);
                        }
                    } else {
                        this.locationData.add(curr);
                        prev = curr;
                    }

                }
            }
        } catch (IOException e) {
            showMessage("Error reading coordinates from file");
            Log.e(TAG, "Error reading coordinates from file", e);
        }
    }

    private void processLocationData() {
        laps = LapDetection.getLaps(locationData, googleMap, polylines);
        if (!laps.isEmpty()) {
            updateUI();
            nextButton.setOnClickListener(v -> {
                currentLapIndex = (currentLapIndex + 1) % laps.size();
                updateUI();
            });
        } else {
            showMessage("No laps detected");
        }
    }

    private void updateUI() {
        MapDrawing.drawLap(googleMap, laps.get(currentLapIndex).getLocationData(), polylines, markers);
        textViewLapTime.setText(String.format("Lap time: %s", formatLapTime(laps.get(currentLapIndex).getLapTime())));
        textViewLaps.setText(String.format("Total laps done: %d", laps.size()));
        textCurrentLap.setText(String.format("Viewing lap: %d", currentLapIndex + 1));
    }

    private String formatLapTime(double lapTimeInSeconds) {
        int minutes = (int) (lapTimeInSeconds / 60);
        double seconds = lapTimeInSeconds % 60;
        return String.format("%d:%06.3f", minutes, seconds);
    }

    private void showMessage(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_message, null);
        builder.setView(dialogView);

        TextView textView = dialogView.findViewById(R.id.textView);
        textView.setText(message);

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}