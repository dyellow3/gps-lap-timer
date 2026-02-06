package com.example.gpslaptimer.models;

/**
 * Immutable GPS data point — replaces android.location.Location
 * so that :shared has zero Android framework dependencies.
 */
public class TrackPoint {
    private final double latitude;
    private final double longitude;
    private final float speed;
    private final long time; // milliseconds

    public TrackPoint(double latitude, double longitude, float speed, long time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.speed = speed;
        this.time = time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public float getSpeed() {
        return speed;
    }

    public long getTime() {
        return time;
    }

    /**
     * Haversine distance between two TrackPoints in meters.
     * Replaces Location.distanceTo() for the 0.5m distance filter.
     */
    public static double distanceMeters(TrackPoint a, TrackPoint b) {
        double earthRadius = 6371000; // meters
        double lat1 = Math.toRadians(a.latitude);
        double lat2 = Math.toRadians(b.latitude);
        double dLat = Math.toRadians(b.latitude - a.latitude);
        double dLon = Math.toRadians(b.longitude - a.longitude);

        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2)
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(h), Math.sqrt(1 - h));

        return earthRadius * c;
    }
}
