package com.example.gpslaptimer.utils;

import com.example.gpslaptimer.models.TrackPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses CSV lines of format "lat,lon,speed,elapsedTimeSeconds"
 * into a List of TrackPoints, applying a minimum-distance filter.
 *
 * Reusable by phone app, Wear app, and JVM tests.
 */
public class TrackPointParser {

    /**
     * @param in           CSV input stream (one point per line)
     * @param minDistanceM minimum distance between consecutive points (meters).
     *                     Pass 0 to keep every point.
     */
    public static List<TrackPoint> parse(InputStream in, double minDistanceM) throws IOException {
        List<TrackPoint> points = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            TrackPoint prev = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                double lat = Double.parseDouble(parts[0]);
                double lon = Double.parseDouble(parts[1]);
                float speed = Float.parseFloat(parts[2]);
                long timeMs = (long) (Double.parseDouble(parts[3]) * 1000);

                TrackPoint curr = new TrackPoint(lat, lon, speed, timeMs);

                if (prev != null && minDistanceM > 0) {
                    if (TrackPoint.distanceMeters(prev, curr) > minDistanceM) {
                        points.add(curr);
                        prev = curr;
                    }
                } else {
                    points.add(curr);
                    prev = curr;
                }
            }
        }
        return points;
    }
}
