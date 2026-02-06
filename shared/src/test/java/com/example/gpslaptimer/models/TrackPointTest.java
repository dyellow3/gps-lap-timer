package com.example.gpslaptimer.models;

import org.junit.Test;

import static org.junit.Assert.*;

public class TrackPointTest {

    @Test
    public void constructorAndGetters() {
        TrackPoint tp = new TrackPoint(33.45, -111.94, 12.5f, 5000L);
        assertEquals(33.45, tp.getLatitude(), 1e-9);
        assertEquals(-111.94, tp.getLongitude(), 1e-9);
        assertEquals(12.5f, tp.getSpeed(), 1e-6);
        assertEquals(5000L, tp.getTime());
    }

    @Test
    public void distanceSamePoint_isZero() {
        TrackPoint p = new TrackPoint(33.45, -111.94, 0f, 0L);
        assertEquals(0.0, TrackPoint.distanceMeters(p, p), 1e-9);
    }

    @Test
    public void distanceOneDegreeLatitude_approx111km() {
        TrackPoint a = new TrackPoint(0.0, 0.0, 0f, 0L);
        TrackPoint b = new TrackPoint(1.0, 0.0, 0f, 0L);
        double dist = TrackPoint.distanceMeters(a, b);
        // One degree of latitude ≈ 111,195 m. Allow 1% tolerance.
        assertEquals(111195, dist, 1200);
    }

    @Test
    public void distanceSmall_belowFilter() {
        // Two points ~0.3m apart (same GPS noise magnitude)
        TrackPoint a = new TrackPoint(33.3618272, -111.6996352, 10f, 0L);
        TrackPoint b = new TrackPoint(33.3618275, -111.6996352, 10f, 1000L);
        double dist = TrackPoint.distanceMeters(a, b);
        assertTrue("Very close points should be < 1m", dist < 1.0);
    }
}
