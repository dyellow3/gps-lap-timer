package com.example.gpslaptimer.utils;

import android.location.Location;
import android.util.Log;
import android.util.Pair;

import com.example.gpslaptimer.config.LapDetectionConfig;
import com.example.gpslaptimer.models.Grid;
import com.example.gpslaptimer.models.GridCell;
import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.LapDetectionResult;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class LapDetection {
    private static final String TAG = "LapDetectionUtil";

    public static LapDetectionResult getLaps(List<Location> locations, List<Double> gridBounds, LapDetectionConfig config) {
        // Get the finish line
        List<LatLng> finishLine = getFinishLine(locations, gridBounds, config);

        double fastestTime = Double.POSITIVE_INFINITY;
        Lap fastestLap = null;

        if (finishLine == null) {
            Log.d(TAG, "Could not detect start / finish points");
            return new LapDetectionResult(new ArrayList<>(), null, null);
        }

        LatLng finishLineStartPoint = finishLine.get(0);
        LatLng finishLineEndPoint = finishLine.get(1);

        // Now that we have finish line, we can find when it is crossed, and mark that as a lap
        List<Lap> laps = new ArrayList<>();
        int startIndex = 0;
        double carryOverTime = 0;

        // Loop through each point, looking at if the line between previous and current point intersects finish
        for (int i = 1; i < locations.size(); i++) {
            LatLng prevPoint = new LatLng(locations.get(i - 1).getLatitude(), locations.get(i - 1).getLongitude());
            LatLng currPoint = new LatLng(locations.get(i).getLatitude(), locations.get(i).getLongitude());
            LatLng intersectionPoint = getLineSegmentIntersection(prevPoint, currPoint, finishLineStartPoint, finishLineEndPoint);

            // Intersection detected
            if (intersectionPoint != null && i - startIndex > 2) {
                List<Location> lap = new ArrayList<>(locations.subList(startIndex, i + 1));
                Pair<Double, Double> lapTime = getLapTime(lap, finishLineStartPoint, finishLineEndPoint);
                if (startIndex != 0) {
                    Lap currLap = new Lap(lap, lapTime.first + carryOverTime, laps.size());
                    if ((lapTime.first + carryOverTime) < fastestTime) {
                        fastestLap = currLap;
                        fastestTime = lapTime.first + carryOverTime;
                    }
                    laps.add(currLap);
                } else {
                    laps.add(new Lap(lap, 0.0, laps.size()));
                }
                carryOverTime = lapTime.second;
                startIndex = i;
            }
        }

        // Add leftover lap data
        if (startIndex < locations.size() - 1) {
            List<Location> finalLap = new ArrayList<>(locations.subList(startIndex, locations.size()));
            double finalLapTime = 0;
            for (int i = 0; i < finalLap.size(); i++) {
                finalLapTime += 1;
            }
            laps.add(new Lap(finalLap, finalLapTime + carryOverTime, laps.size()));
        }

        // Add variance from fastest to each lap
        for (int i = 1; i < laps.size() - 1; i++) {
            Lap curr = laps.get(i);
            curr.setLapVariation(curr.getLapTime() - fastestTime);
        }

        if (laps.isEmpty()) {
            Log.d(TAG, "Could not detect any laps");
            return new LapDetectionResult(new ArrayList<>(), null, null);
        }
        return new LapDetectionResult(laps, fastestLap, finishLine);
    }


    public static List<LatLng> getFinishLine(List<Location> locations, List<Double> gridBounds, LapDetectionConfig config) {
        double LINE_LENGTH = config.getFinishLength();
        double GRID_SIZE = config.getGridSize();
        double DIRECTION_TOLERANCE = Math.toRadians(config.getDirectionTolerance());

        Grid grid = Grid.createGrid(locations, gridBounds, GRID_SIZE, DIRECTION_TOLERANCE);

        if (grid == null) {
            return null;
        }

        GridCell maxCountCell = grid.getMaxCountCell();

        if (maxCountCell == null || maxCountCell.getCount() <= 1) {
            return null;
        }
        
        LatLng centerLatLng = getAveragePoint(maxCountCell.getPoints());
        LatLng directionVector = maxCountCell.getDirectionVector();

        // Perpendicular vector
        double perpendicularLat = -directionVector.longitude;
        double perpendicularLng = directionVector.latitude;
        double distanceInDegrees = LINE_LENGTH / 111111; // Approximate conversion factor: 1 degree ≈ 111,111 meters

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

    public static Pair<Double, Double> getLapTime(List<Location> lap, LatLng finishLineStartPoint, LatLng finishLineEndPoint) {
        double lapTime = 0;
        double carryOverTime = 0;

        int n = lap.size();
        if (n > 2) {
            Location firstPoint = lap.get(0);
            Location lastPoint = lap.get(n - 1);
            Location secondLastPoint = lap.get(n - 2);

            LatLng lastLatLng = new LatLng(lastPoint.getLatitude(), lastPoint.getLongitude());
            LatLng secondLastLatLng = new LatLng(secondLastPoint.getLatitude(), secondLastPoint.getLongitude());

            double segmentDistance = calculateDistance(secondLastLatLng, lastLatLng);
            double segmentTime = (lastPoint.getTime() - secondLastPoint.getTime());

            LatLng intersectionPoint = getLineSegmentIntersection(secondLastLatLng, lastLatLng, finishLineStartPoint, finishLineEndPoint);
            if (intersectionPoint != null) {

                double distanceToIntersection = calculateDistance(secondLastLatLng, intersectionPoint);
                double timeToIntersection = (distanceToIntersection / segmentDistance) * segmentTime;

                // Simple linear interpolation
                lapTime = timeToIntersection + (secondLastPoint.getTime() - firstPoint.getTime());
                carryOverTime = segmentTime - timeToIntersection;

            } else {
                lapTime = (lastPoint.getTime() - firstPoint.getTime());
            }
        }
        return new Pair<>(lapTime / 1000.0, carryOverTime / 1000.0); //s to ms
    }

    public static LatLng getAveragePoint(List<Location> points) {
        double sumLat = 0;
        double sumLng = 0;

        for (Location point : points) {
            sumLat += point.getLatitude();
            sumLng += point.getLongitude();
        }

        return new LatLng(sumLat / points.size(), sumLng / points.size());
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
            return null;
        }
        
        double longitude = (b2 * c1 - b1 * c2) / determinant;
        double latitude = (a1 * c2 - a2 * c1) / determinant;

        LatLng intersection = new LatLng(latitude, longitude);

        if (onSegment(p1, intersection, q1) && onSegment(p2, intersection, q2)) {
            return intersection;
        } else {
            return null;
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
