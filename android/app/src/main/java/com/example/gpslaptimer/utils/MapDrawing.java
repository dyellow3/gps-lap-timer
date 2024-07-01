package com.example.gpslaptimer.utils;

import android.graphics.Color;

import com.example.gpslaptimer.models.LocationData;
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
                .width(5);
        googleMap.addPolyline(polylineOptions);
    }

    public static void drawLap(GoogleMap googleMap, List<LocationData> lap, List<Polyline> polylines, List<Marker> markers, double minSpeed, double maxSpeed) {
        removePolylines(polylines);
        removeMarkers(markers);
        addMarker(googleMap, lap.get(0).getCoordinate(), "Start", markers);
        addMarker(googleMap, lap.get(lap.size() - 1).getCoordinate(), "Finish", markers);

        for (int i = 0; i < lap.size() - 1; i++) {
            LocationData start = lap.get(i);
            LocationData end = lap.get(i + 1);

            PolylineOptions polylineOptions = new PolylineOptions()
                    .add(start.getCoordinate())
                    .add(end.getCoordinate())
                    .width(5)
                    .color(getColorForSpeed(start.getSpeed(), minSpeed, maxSpeed));

            Polyline polyline = googleMap.addPolyline(polylineOptions);
            polylines.add(polyline);
        }
    }

    public static void drawAllCoordinates(GoogleMap googleMap, List<LocationData> locationData, List<Polyline> polylines) {
        removePolylines(polylines);

        PolylineOptions polylineOptions = new PolylineOptions();
        for (LocationData data : locationData) {
            polylineOptions.add(data.getCoordinate());
        }
        polylineOptions.color(getRandomColor()).width(5);

        Polyline polyline = googleMap.addPolyline(polylineOptions);
        polylines.add(polyline);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(locationData.get((0)).getCoordinate().latitude, locationData.get((0)).getCoordinate().longitude), 15));
    }

    private static void addMarker(GoogleMap googleMap, LatLng position, String markerName, List<Marker> markers) {
        Marker marker = googleMap.addMarker(new MarkerOptions()
                .position(position)
                .title(markerName)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        markers.add(marker);
    }

    private static void removeMarkers(List<Marker> markers) {
        for (Marker marker : markers) {
            marker.remove();
        }
        markers.clear();
    }

    private static void removePolylines(List<Polyline> polylines) {
        for (Polyline polyline : polylines) {
            polyline.remove();
        }
        polylines.clear();
    }

    private static int getRandomColor() {
        Random rnd = new Random();
        return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
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
