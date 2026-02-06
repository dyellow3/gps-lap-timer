package com.example.gpslaptimer.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class Grid {
    private GridCell[][] cells;
    private double minLon, maxLon, minLat, maxLat;
    private double gridSize;
    private int width, height;
    private GridCell maxCountCell;

    private Grid(double minLon, double maxLon, double minLat, double maxLat, double squareSize) {
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.gridSize = squareSize;

        this.width = (int) Math.ceil(degreesToMeters(maxLon - minLon) / gridSize);
        this.height = (int) Math.ceil(degreesToMeters(maxLat - minLat) / gridSize);
        this.cells = new GridCell[height][width];
        this.maxCountCell = null;
    }

    public GridCell getMaxCountCell() {
        return maxCountCell;
    }

    public static Grid createGrid(List<TrackPoint> locations, List<Double> gridBounds, double squareSize, double directionTolerance) {
        double minLon = gridBounds.get(0), maxLon = gridBounds.get(1);
        double minLat = gridBounds.get(2), maxLat = gridBounds.get(3);

        int gridWidth = (int) Math.ceil(degreesToMeters(maxLon - minLon) / squareSize);
        int gridHeight = (int) Math.ceil(degreesToMeters(maxLat - minLat) / squareSize);

        if(gridWidth < 1 || gridHeight < 1) {
            return null;
        }

        Grid grid = new Grid(minLon, maxLon, minLat, maxLat, squareSize);

        for (int i = 0; i < locations.size() - 1; i++) {
            TrackPoint curr = locations.get(i);
            TrackPoint next = locations.get(i + 1);

            int x = (int) (degreesToMeters(curr.getLongitude() - minLon) / squareSize);
            int y = (int) (degreesToMeters(curr.getLatitude() - minLat) / squareSize);

            LatLng directionVector = new LatLng(next.getLatitude() - curr.getLatitude(), next.getLongitude() - curr.getLongitude());
            double magnitude = Math.sqrt(directionVector.latitude * directionVector.latitude + directionVector.longitude * directionVector.longitude);
            directionVector = new LatLng(directionVector.latitude / magnitude, directionVector.longitude / magnitude);

            if (grid.cells[y][x] == null) {
                grid.cells[y][x] = new GridCell();
                grid.cells[y][x].setDirectionVector(directionVector);
                grid.cells[y][x].addPoint(curr);
            } else if (dotProduct(directionVector, grid.cells[y][x].getDirectionVector()) >= directionTolerance)  {
                grid.cells[y][x].addPoint(curr);
                if(grid.maxCountCell == null || grid.cells[y][x].getCount() > grid.maxCountCell.getCount()) {
                    grid.maxCountCell = grid.cells[y][x];
                }
            }
        }
        return grid;
    }

    private static double dotProduct(LatLng v1, LatLng v2) {
        return v1.latitude * v2.latitude + v1.longitude * v2.longitude;
    }

    private static double degreesToMeters(double degree) {
        return degree * 111319.9;
    }
}