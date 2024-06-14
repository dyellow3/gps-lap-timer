package com.example.gpslaptimer.models;

import com.google.android.gms.maps.model.LatLng;

public class LocationData {
    private LatLng coordinate;
    private double speed;
    private int weight;

    public LocationData(LatLng coordinate, double speed, int weight) {
        this.coordinate = coordinate;
        this.speed = speed;
        this.weight = weight;
    }

    public LatLng getCoordinate() {
        return coordinate;
    }

    public double getSpeed() {
        return speed;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
    public int getWeight() {
        return weight;
    }
}
