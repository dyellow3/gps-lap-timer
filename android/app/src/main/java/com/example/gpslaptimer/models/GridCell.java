package com.example.gpslaptimer.models;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class GridCell {
    private List<Location> points = new ArrayList<>();
    private LatLng directionVector = null;

    public void addPoint(Location point) {
        points.add(point);
    }

    public int getCount() {
        return points.size();
    }

    public List<Location> getPoints() {
        return points;
    }

    public LatLng getDirectionVector() {
        return directionVector;
    }
    public void setDirectionVector(LatLng directionVector) {
        this.directionVector = directionVector;
    }
}
