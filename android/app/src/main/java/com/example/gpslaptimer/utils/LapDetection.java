package com.example.gpslaptimer.utils;

import android.util.Log;
import android.util.Pair;

import com.example.gpslaptimer.models.Grid;
import com.example.gpslaptimer.models.GridCell;
import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.LocationData;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import java.util.ArrayList;
import java.util.List;
public class LapDetection {
    private static final String TAG = "LapDetectionUtil";
    private static final double GPS_HZ = 1; // Using Neo-6m
    private static final double GRID_SIZE = 1.0;
    private static final double DIRECTION_TOLERANCE = Math.toRadians(45);

    public static List<Lap> getLaps(List<LocationData> locationData, GoogleMap googleMap, List<Polyline> polylines, List<Double> gridBounds, List<Double> longitudes, List<Double> latitudes) {
        List<LatLng> finishLine = getFinishLine(locationData, gridBounds, 8.0);

        if(finishLine != null) {
            MapDrawing.drawLine(googleMap, finishLine);
            LatLng finishLineStartPoint = finishLine.get(0);
            LatLng finishLineEndPoint = finishLine.get(1);

            // Now that we have finish line, we can find when it is crossed, and mark that as a lap
            List<Lap> laps = new ArrayList<>();
            int startIndex = 0;
            double carryOverTime = 0;

            for (int i = 1; i < locationData.size(); i++) {
                LatLng prevPoint = locationData.get(i - 1).getCoordinate();
                LatLng currPoint = locationData.get(i).getCoordinate();
                LatLng intersectionPoint = getLineSegmentIntersection(prevPoint, currPoint, finishLineStartPoint, finishLineEndPoint);

                if (intersectionPoint != null && i - startIndex > 2) {
                    List<LocationData> lap = new ArrayList<>(locationData.subList(startIndex, i + 1));
                    Pair<Double, Double> lapTime = getLapTime(lap, finishLineStartPoint, finishLineEndPoint);
                    laps.add(new Lap(lap, lapTime.first + carryOverTime));
                    carryOverTime = lapTime.second;
                    startIndex = i;
                }
            }

            // Add leftover lap data
            if (startIndex < locationData.size() - 1) {
                List<LocationData> finalLap = new ArrayList<>(locationData.subList(startIndex, locationData.size()));
                double finalLapTime = 0;
                for(int i = 0; i < finalLap.size(); i++) {
                    finalLapTime += finalLap.get(i).getWeight();
                }
                laps.add(new Lap(finalLap, finalLapTime + carryOverTime));
            }


            if(laps.isEmpty()) {
                Log.d(TAG, "Could not detect any laps");
                MapDrawing.drawAllCoordinates(googleMap, locationData, polylines);
            }
            return laps;
        } else {
            Log.d(TAG, "Could not detect start / finish points");
            MapDrawing.drawAllCoordinates(googleMap, locationData, polylines);
            return new ArrayList<>();
        }

    }


    public static List<LatLng> getFinishLine(List<LocationData> locationData, List<Double> gridBounds, double lineLength) {
        Grid grid = Grid.createGrid(locationData, gridBounds, GRID_SIZE, DIRECTION_TOLERANCE);

        if(grid != null) {
            GridCell maxCountCell = grid.getMaxCountCell();
            if(maxCountCell != null && maxCountCell.getCount() > 1) {
                LatLng centerLatLng = getAveragePoint(maxCountCell.getPoints());
                LatLng directionVector = maxCountCell.getDirectionVector();

                // Perpendicular vector
                double perpendicularLat = -directionVector.longitude;
                double perpendicularLng = directionVector.latitude;
                double distanceInDegrees = lineLength / 111111; // Approximate conversion factor: 1 degree ≈ 111,111 meters

                // End points of the perpendicular line
                double startLat = centerLatLng.latitude - (perpendicularLat * distanceInDegrees / 2);
                double startLng = centerLatLng.longitude - (perpendicularLng * distanceInDegrees / 2);
                double endLat = centerLatLng.latitude + (perpendicularLat * distanceInDegrees / 2);
                double endLng = centerLatLng.longitude + (perpendicularLng * distanceInDegrees / 2);

                List<LatLng> finishLine = new ArrayList<>();
                finishLine.add(new LatLng(startLat, startLng));
                finishLine.add(new LatLng(endLat, endLng));

                return finishLine;
            }
        }

        return null;
    }

    public static LatLng getAveragePoint(List<LocationData> points) {
        double sumLat = 0;
        double sumLng = 0;

        for (LocationData point : points) {
            sumLat += point.getCoordinate().latitude;
            sumLng += point.getCoordinate().longitude;
        }

        return new LatLng(sumLat / points.size(), sumLng / points.size());
    }

    public static Pair<Double, Double> getLapTime(List<LocationData> lap, LatLng finishLineStartPoint, LatLng finishLineEndPoint) {
        double lapTime = 0;
        double carryOverTime = 0;

        int n = lap.size();
        if (n > 2) {
            LocationData lastPoint = lap.get(n-1);
            LocationData secondLastPoint = lap.get(n-2);

            double segmentDistance = calculateDistance(secondLastPoint.getCoordinate(), lastPoint.getCoordinate());
            double segmentTime = 1 / GPS_HZ;

            /*
            double initialSpeed = secondLastPoint.getSpeed();
            double finalSpeed = lastPoint.getSpeed();

            // Constants for quadratic equations
            double a1 = 1.0/3.0, b1 = 1.0/2.0, c1 = segmentDistance-initialSpeed;
            double a2 = 1.0, b2 = 1.0, c2 = finalSpeed-initialSpeed;

            // Solving system of quadratic equations to get 'a' and velocity 'b'
            double b = (c1 - a1 * c2 / a2) / (b1 - a1 * b2 / a2);
            double a = (c2 - b2 * b) / a2;

            // We now how can equation of the form a*t^2 + b*t + c
            // such that an integral over t = 0 to GPS_HZ will result in the segment distance
             */

            LatLng intersectionPoint = getLineSegmentIntersection(secondLastPoint.getCoordinate(), lastPoint.getCoordinate(), finishLineStartPoint, finishLineEndPoint);
            if (intersectionPoint != null) {

                double distanceToIntersection = calculateDistance(secondLastPoint.getCoordinate(), intersectionPoint);
                double timeToIntersection = (distanceToIntersection / segmentDistance) * segmentTime;

                // Simple linear interpolation
                lapTime = timeToIntersection;
                for(int i = 0; i < n-2; i++) {
                    lapTime += lap.get(i).getWeight();
                    if(lap.get(i).getWeight() > 1) {
                        Log.d(TAG, "Weight at " + i + " is " + lap.get(i).getWeight());
                    }
                }
                carryOverTime = segmentTime - timeToIntersection;

                /*
                // Solve for t such that integral over a*t^2 + b*t + c from 0 to t will equal distance to intersection
                double timeToIntersection = solveTimeToIntersection(a, b, initialSpeed, distanceToIntersection);
                lapTime = timeToIntersection + (n-2)/GPS_HZ;
                carryOverTime = 1 - timeToIntersection;
                */
            }
        }
        return new Pair<>(lapTime, carryOverTime);
    }

    private static LatLng getLineSegmentIntersection(LatLng p1, LatLng q1, LatLng p2, LatLng q2) {
        double a1 = q1.latitude - p1.latitude;
        double b1 = p1.longitude - q1.longitude;
        double c1 = a1 * p1.longitude + b1 * p1.latitude;

        double a2 = q2.latitude - p2.latitude;
        double b2 = p2.longitude - q2.longitude;
        double c2 = a2 * p2.longitude + b2 * p2.latitude;

        double determinant = a1 * b2 - a2 * b1;

        if (determinant == 0) {
            // The lines are parallel
            return null;
        } else {
            double longitude = (b2 * c1 - b1 * c2) / determinant;
            double latitude = (a1 * c2 - a2 * c1) / determinant;

            LatLng intersection = new LatLng(latitude, longitude);

            if (onSegment(p1, intersection, q1) && onSegment(p2, intersection, q2)) {
                return intersection;
            } else {
                return null;
            }
        }
    }

    private static boolean onSegment(LatLng p, LatLng q, LatLng r) {
        double epsilon = 1e-9;
        return q.latitude <= Math.max(p.latitude, r.latitude) + epsilon &&
                q.latitude >= Math.min(p.latitude, r.latitude) - epsilon &&
                q.longitude <= Math.max(p.longitude, r.longitude) + epsilon &&
                q.longitude >= Math.min(p.longitude, r.longitude) - epsilon;
    }

    public static double calculateDistance(LatLng point1, LatLng point2) {
        double earthRadius = 6371000;
        double lat1 = Math.toRadians(point1.latitude);
        double lon1 = Math.toRadians(point1.longitude);
        double lat2 = Math.toRadians(point2.latitude);
        double lon2 = Math.toRadians(point2.longitude);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = earthRadius * c;
        return distance;
    }

}