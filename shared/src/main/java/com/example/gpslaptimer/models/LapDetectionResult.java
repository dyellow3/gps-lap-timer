package com.example.gpslaptimer.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

/**
 * Result wrapper for lap detection output.
 * Bundles laps, fastest lap, and finish line geometry
 * so callers get everything in one return value.
 */
public class LapDetectionResult {
    private final List<Lap> laps;
    private final Lap fastestLap;
    private final List<LatLng> finishLine;

    public LapDetectionResult(List<Lap> laps, Lap fastestLap, List<LatLng> finishLine) {
        this.laps = laps;
        this.fastestLap = fastestLap;
        this.finishLine = finishLine;
    }

    public List<Lap> getLaps() {
        return laps;
    }

    public Lap getFastestLap() {
        return fastestLap;
    }

    public List<LatLng> getFinishLine() {
        return finishLine;
    }
}
