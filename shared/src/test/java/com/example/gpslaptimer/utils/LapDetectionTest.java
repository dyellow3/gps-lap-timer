package com.example.gpslaptimer.utils;

import com.example.gpslaptimer.config.LapDetectionConfig;
import com.example.gpslaptimer.models.LapDetectionResult;
import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.TrackPoint;

import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class LapDetectionTest {

    /** Default config matching SettingsManager defaults */
    private static final LapDetectionConfig DEFAULT_CONFIG =
            new LapDetectionConfig(8.0f, 1.0f, 15.0f);

    // ---------- helpers ----------

    private List<TrackPoint> loadFixture(String name) throws IOException {
        InputStream is = getClass().getClassLoader().getResourceAsStream(name);
        assertNotNull("Fixture not found: " + name, is);
        return TrackPointParser.parse(is, 0.5);
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

    // ---------- real data tests ----------

    @Test
    public void loopTrack1_detectsMultipleLaps() throws IOException {
        List<TrackPoint> points = loadFixture("loop-track-1.csv");
        List<Double> bounds = computeBounds(points);

        LapDetectionResult result = LapDetection.getLaps(points, bounds, DEFAULT_CONFIG);

        assertNotNull(result);
        assertTrue("Should detect at least 2 laps (including partial first/last)",
                result.getLaps().size() >= 2);
        assertNotNull("Finish line should be detected", result.getFinishLine());
    }

    @Test
    public void loopTrack2_detectsLapsWithReasonableTimes() throws IOException {
        List<TrackPoint> points = loadFixture("loop-track-2.csv");
        List<Double> bounds = computeBounds(points);

        LapDetectionResult result = LapDetection.getLaps(points, bounds, DEFAULT_CONFIG);

        assertNotNull(result);
        assertTrue("Should detect laps", result.getLaps().size() >= 2);

        if (result.getFastestLap() != null) {
            double fastest = result.getFastestLap().getLapTime();
            assertTrue("Fastest lap should be positive", fastest > 0);
            assertTrue("Fastest lap should be < 600s (10 min)", fastest < 600);
        }
    }

    // ---------- edge cases ----------

    @Test
    public void emptyList_returnsNoLaps() {
        // Tight bounds so Grid doesn't try to allocate a huge array
        LapDetectionResult result = LapDetection.getLaps(
                Collections.emptyList(),
                Arrays.asList(-111.700, -111.699, 33.360, 33.361),
                DEFAULT_CONFIG);

        assertNotNull(result);
        assertTrue(result.getLaps().isEmpty());
    }

    @Test
    public void singlePoint_returnsNoLaps() {
        List<TrackPoint> one = Collections.singletonList(
                new TrackPoint(33.360, -111.700, 5f, 0L));

        LapDetectionResult result = LapDetection.getLaps(
                one,
                Arrays.asList(-111.7005, -111.6995, 33.3595, 33.3605),
                DEFAULT_CONFIG);

        assertNotNull(result);
        assertTrue(result.getLaps().size() <= 1);
    }

    @Test
    public void twoPoints_returnsNoCompleteLap() {
        List<TrackPoint> two = Arrays.asList(
                new TrackPoint(33.360, -111.700, 5f, 0L),
                new TrackPoint(33.361, -111.700, 5f, 1000L));

        LapDetectionResult result = LapDetection.getLaps(
                two,
                Arrays.asList(-111.7005, -111.6995, 33.3595, 33.3615),
                DEFAULT_CONFIG);

        assertNotNull(result);
        assertTrue(result.getLaps().size() <= 1);
    }
}
