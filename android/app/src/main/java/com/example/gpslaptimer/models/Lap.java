package com.example.gpslaptimer.models;

import java.util.List;

public class Lap {
    private List<LocationData> locationData;
    private double lapTime;

    public Lap(List<LocationData> locationData, double lapTime) {
        this.locationData = locationData;
        this.lapTime = lapTime;
    }

    public List<LocationData> getLocationData() {
        return locationData;
    }

    public double getLapTime() {
        return lapTime;
    }


}
