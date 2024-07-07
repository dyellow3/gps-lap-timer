package com.example.gpslaptimer.utils;

import android.graphics.Color;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.List;
import java.util.Random;

public class MapDrawing {
    public static void drawLine(GoogleMap googleMap, List<LatLng> linePoints) {
        PolylineOptions polylineOptions = new PolylineOptions()
                .add(linePoints.get(0))
                .add(linePoints.get(1))
                .color(Color.RED)
                .width(8);
        googleMap.addPolyline(polylineOptions);
    }

    public static void drawLap(GoogleMap googleMap, List<Location> lap, List<Polyline> polylines, double minSpeed, double maxSpeed) {
        removePolylines(polylines);

        for (int i = 0; i < lap.size() - 1; i++) {
            Location start = lap.get(i);
            Location end = lap.get(i + 1);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(new LatLng(start.getLatitude(), start.getLongitude()))
                    .add(new LatLng(end.getLatitude(), end.getLongitude()))
                    .width(5)
                    .color(getColorForSpeed(start.getSpeed(), minSpeed, maxSpeed));

            Polyline polyline = googleMap.addPolyline(polylineOptions);
            polylines.add(polyline);
        }
    }

    private static void removePolylines(List<Polyline> polylines) {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
    }

    private static int getColorForSpeed(double speed, double minSpeed, double maxSpeed) {
        speed = Math.max(minSpeed, Math.min(speed, maxSpeed));

        double normalizedSpeed = (speed - minSpeed) / (maxSpeed - minSpeed);

        float hue = (float) (normalizedSpeed * 120); // 0 = red, 120 = green
        float saturation = 1f;
        float value = 1f;

        return Color.HSVToColor(new float[]{hue, saturation, value});
    }
}
