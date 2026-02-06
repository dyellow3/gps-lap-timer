package com.example.gpslaptimer.models;

import java.util.List;

public class Lap {
    private List<TrackPoint> locations;
    private double lapTime;
    private double lapVariation;
    private int lapNumber;

    public Lap(List<TrackPoint> locations, double lapTime, int lapNumber) {
        this.locations = locations;
        this.lapTime = lapTime;
        this.lapNumber = lapNumber;
    }

    public List<TrackPoint> getLocationData() {
        return locations;
    }

    public double getLapTime() {
        return lapTime;
    }

    public double getVariation() { return lapVariation; }

    public void setLapVariation(double lapVariation) { this.lapVariation = lapVariation; }

    public int getLapNumber() { return lapNumber; }


}
