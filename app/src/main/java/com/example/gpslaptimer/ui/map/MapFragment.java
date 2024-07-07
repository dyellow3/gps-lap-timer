package com.example.gpslaptimer.ui.map;

import android.app.AlertDialog;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gpslaptimer.adapters.LapAdapter;
import com.example.gpslaptimer.ui.settings.SettingsViewModel;
import com.example.gpslaptimer.utils.LapDetection;
import com.example.gpslaptimer.utils.MapDrawing;
import com.example.gpslaptimer.R;
import com.example.gpslaptimer.models.Lap;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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
    private GoogleMap googleMap;
    private MapViewModel mapViewModel;
    private SettingsViewModel settingsViewModel;


    private List<Location> locations = new ArrayList<>();
    private List<Double> gridBounds = new ArrayList<>();
    private List<Polyline> polylines = new ArrayList<>();
    private List<Lap> laps;
    Lap fastestLap = null;

    private TextView textViewFastestLapTime;
    private TextView textViewFastestLap;
    private TextView textViewLapNumber, textViewLapTime, textViewGap;


    private int pointsDetected;
    private double minLon, maxLon, minLat, maxLat;


    private double minSpeed = Double.POSITIVE_INFINITY;
    private double maxSpeed = Double.NEGATIVE_INFINITY;

    View rootView;

    public static MapFragment newInstance() {
        return new MapFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_map, container, false);

        textViewFastestLapTime = rootView.findViewById(R.id.textViewFastestLapTime);
        textViewFastestLap = rootView.findViewById(R.id.textViewFastestLap);

        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        mapViewModel.getFileName().observe(getViewLifecycleOwner(), this::readAndStoreCoordinates);
        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

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
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

        if (!locations.isEmpty()) {
            requireActivity().runOnUiThread((this::processLocationData));
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locations.get((0)).getLatitude(), locations.get((0)).getLongitude()), 17));
        } else {
            showMessage("No location data detected");
            Log.d(TAG, "No location data detected");
        }

        RecyclerView recyclerView = rootView.findViewById(R.id.recyclerViewLaps);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        LapAdapter adapter = new LapAdapter(laps, new LapAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String lapNumber) {
                drawLap(Integer.parseInt(lapNumber));
            }
        });
        recyclerView.setAdapter(adapter);
    }

    private void readAndStoreCoordinates(String fileName) {
        minLon = Double.POSITIVE_INFINITY;
        maxLon = Double.NEGATIVE_INFINITY;
        minLat = Double.POSITIVE_INFINITY;
        maxLat = Double.NEGATIVE_INFINITY;

        pointsDetected = 0;
        locations.clear();
        gridBounds.clear();

        File directory = getContext().getExternalFilesDir(null);
        File file = new File(directory, fileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            Location prev = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    double latitude = Double.parseDouble(parts[0]);
                    double longitude = Double.parseDouble(parts[1]);
                    float speed = Float.parseFloat(parts[2]);
                    double elapsedTime = Double.parseDouble(parts[3]);

                    minLon = Math.min(minLon, longitude);
                    maxLon = Math.max(maxLon, longitude);
                    minLat = Math.min(minLat, latitude);
                    maxLat = Math.max(maxLat, latitude);

                    minSpeed = Math.min(minSpeed, speed);
                    maxSpeed = Math.max(maxSpeed, speed);

                    Location curr = new Location("");
                    curr.setLatitude(latitude);
                    curr.setLongitude(longitude);
                    curr.setSpeed(speed);
                    curr.setTime((long) (elapsedTime * 1000)); // s to ms
                    pointsDetected++;

                    if(prev != null) {
                        double distance = prev.distanceTo(curr);
                        if(distance > 0.5) {
                            this.locations.add(curr);
                            prev = curr;
                        }
                    } else {
                        this.locations.add(curr);
                        prev = curr;
                    }

                }
            }
            gridBounds.add(minLon);
            gridBounds.add(maxLon);
            gridBounds.add(minLat);
            gridBounds.add(maxLat);
        } catch (IOException e) {
            showMessage("Error reading coordinates from file");
            Log.e(TAG, "Error reading coordinates from file", e);
        }
    }

    private void processLocationData() {
        Pair<List<Lap>, Lap> lapData = LapDetection.getLaps(locations, googleMap, gridBounds, settingsViewModel);

        laps = lapData.first;
        fastestLap = lapData.second;

        if (!laps.isEmpty()) {
            if(fastestLap != null) {
                textViewFastestLapTime.setText(String.format("%.3f", fastestLap.getLapTime()));
                textViewFastestLap.setText("Fastest Lap: " + fastestLap.getLapNumber());

                // Headers
                textViewLapNumber = rootView.findViewById(R.id.textViewLapNumber);
                textViewLapNumber.setText("Lap");
                textViewLapTime = rootView.findViewById(R.id.textViewLapTime);
                textViewLapTime.setText("Time");
                textViewGap = rootView.findViewById(R.id.textViewGap);
                textViewGap.setText("Gap");

                drawLap(fastestLap.getLapNumber());
            }
            else {
                textViewFastestLapTime.setText("0.00");
                textViewFastestLap.setText("N/A");
                drawAll();
            }
        } else {
            showMessage("No laps detected");
            drawAll();
            textViewFastestLapTime.setText(String.format("Laps detected: %d (%d points)", laps.size(), pointsDetected));
        }

    }

    private void drawLap(int lapIndex) {
        MapDrawing.drawLap(googleMap, laps.get(lapIndex).getLocationData(), polylines, minSpeed, maxSpeed);
    }

    private void drawAll() {
        MapDrawing.drawLap(googleMap, locations, polylines, minSpeed, maxSpeed); // This draws all data, all points are passed in as a "lap"
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