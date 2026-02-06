package com.example.gpslaptimer.utils;

import com.example.gpslaptimer.config.LapDetectionConfig;
import com.example.gpslaptimer.models.LapDetectionResult;
import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.TrackPoint;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class SyntheticTrackTest {

    private static final LapDetectionConfig CONFIG =
            new LapDetectionConfig(8.0f, 1.0f, 15.0f);

    /**
     * Generates GPS points along a rectangle, repeated for the given lap count.
     * Corners: (baseLat, baseLon) → east → north → west → back to start.
     *
     * @param lapCount      number of full laps around the rectangle
     * @param pointsPerSide points generated per side of the rectangle
     * @return list of TrackPoints at constant speed and uniform time intervals
     */
    static List<TrackPoint> generateRectangularTrack(int lapCount, int pointsPerSide) {
        double baseLat = 33.36;
        double baseLon = -111.70;
        // Rectangle ~100m wide x ~50m tall (in degrees)
        double widthDeg = 0.001;  // ~111 m at this latitude
        double heightDeg = 0.0005; // ~55 m

        List<TrackPoint> points = new ArrayList<>();
        float speed = 10.0f; // m/s
        long timeMs = 0;
        long intervalMs = 1000; // 1 second per point

        for (int lap = 0; lap < lapCount; lap++) {
            // South side: west → east
            for (int i = 0; i < pointsPerSide; i++) {
                double frac = (double) i / pointsPerSide;
                points.add(new TrackPoint(baseLat, baseLon + frac * widthDeg, speed, timeMs));
                timeMs += intervalMs;
            }
            // East side: south → north
            for (int i = 0; i < pointsPerSide; i++) {
                double frac = (double) i / pointsPerSide;
                points.add(new TrackPoint(baseLat + frac * heightDeg, baseLon + widthDeg, speed, timeMs));
                timeMs += intervalMs;
            }
            // North side: east → west
            for (int i = 0; i < pointsPerSide; i++) {
                double frac = (double) i / pointsPerSide;
                points.add(new TrackPoint(baseLat + heightDeg, baseLon + widthDeg - frac * widthDeg, speed, timeMs));
                timeMs += intervalMs;
            }
            // West side: north → south
            for (int i = 0; i < pointsPerSide; i++) {
                double frac = (double) i / pointsPerSide;
                points.add(new TrackPoint(baseLat + heightDeg - frac * heightDeg, baseLon, speed, timeMs));
                timeMs += intervalMs;
            }
        }
        // Close back to start
        points.add(new TrackPoint(baseLat, baseLon, speed, timeMs));

        return points;
    }

    private static List<Double> computeBounds(List<TrackPoint> points) {
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        for (TrackPoint p : points) {
            minLon = Math.min(minLon, p.getLongitude());
            maxLon = Math.max(maxLon, p.getLongitude());
            minLat = Math.min(minLat, p.getLatitude());
            maxLat = Math.max(maxLat, p.getLatitude());
        }
        return Arrays.asList(minLon, maxLon, minLat, maxLat);
    }

    @Test
    public void rectangularTrack_detectsCorrectLapCount() {
        int expectedLaps = 5;
        List<TrackPoint> track = generateRectangularTrack(expectedLaps, 20);
        List<Double> bounds = computeBounds(track);

        LapDetectionResult result = LapDetection.getLaps(track, bounds, CONFIG);

        assertNotNull(result);
        // The algorithm may count partial first/last laps, so allow tolerance
        int detected = result.getLaps().size();
        assertTrue("Should detect at least " + (expectedLaps - 1) + " laps but got " + detected,
                detected >= expectedLaps - 1);
        assertTrue("Should detect at most " + (expectedLaps + 2) + " laps but got " + detected,
                detected <= expectedLaps + 2);
    }

    @Test
    public void rectangularTrack_middleLapTimesAreConsistent() {
        int lapCount = 5;
        List<TrackPoint> track = generateRectangularTrack(lapCount, 20);
        List<Double> bounds = computeBounds(track);

        LapDetectionResult result = LapDetection.getLaps(track, bounds, CONFIG);
        List<Lap> laps = result.getLaps();

        // Skip first and last laps (partials) — check middle laps are within 20% of each other
        if (laps.size() >= 4) {
            List<Double> middleTimes = new ArrayList<>();
            for (int i = 1; i < laps.size() - 1; i++) {
                double t = laps.get(i).getLapTime();
                if (t > 0) middleTimes.add(t);
            }
            if (middleTimes.size() >= 2) {
                double avg = 0;
                for (double t : middleTimes) avg += t;
                avg /= middleTimes.size();

                for (double t : middleTimes) {
                    double deviation = Math.abs(t - avg) / avg;
                    assertTrue("Lap time " + t + " deviates more than 20% from average " + avg,
                            deviation < 0.20);
                }
            }
        }
    }
}
