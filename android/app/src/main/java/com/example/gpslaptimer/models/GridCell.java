package com.example.gpslaptimer.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class GridCell {
    private List<LocationData> points = new ArrayList<>();
    private LatLng directionVector = null;

    public void addPoint(LocationData point) {
        points.add(point);
    }

    public int getCount() {
        return points.size();
    }

    public List<LocationData> getPoints() {
        return points;
    }

    public LatLng getDirectionVector() {
        return directionVector;
    }
    public void setDirectionVector(LatLng directionVector) {
        this.directionVector = directionVector;
    }
}
