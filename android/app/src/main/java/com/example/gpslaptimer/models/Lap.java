package com.example.gpslaptimer.models;

import android.location.Location;

import java.util.List;

public class Lap {
    private List<Location> locations;
    private double lapTime;

    public Lap(List<Location> locations, double lapTime) {
        this.locations = locations;
        this.lapTime = lapTime;
    }

    public List<Location> getLocationData() {
        return locations;
    }

    public double getLapTime() {
        return lapTime;
    }


}
