package com.example.gpslaptimer.utils;

import android.util.Log;
import android.util.Pair;

import com.example.gpslaptimer.models.Lap;
import com.example.gpslaptimer.models.LocationData;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.List;
public class LapDetection {
    private static final String TAG = "LapDetectionUtil";
    private static final double RADIUS_THRESHOLD = 4.5;
    private static final double GPS_HZ = 1; // Using Neo-6m

    public static List<Lap> getLaps(List<LocationData> locationData, GoogleMap googleMap, List<Polyline> polylines) {
        List<Integer> startFinishPoints = getStartFinishPoints(locationData);

        if(startFinishPoints != null) {
            List<LatLng> finishLine = getFinishLine(locationData, startFinishPoints, 8);
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
                if (intersectionPoint != null ) {
                    List<LocationData> lap = new ArrayList<>(locationData.subList(startIndex, i + 1));
                    Pair<Double, Double> lapTime = getLapTime(lap, finishLineStartPoint, finishLineEndPoint);
                    laps.add(new Lap(lap, lapTime.first + carryOverTime));
                    carryOverTime = lapTime.second;
                    startIndex = i;
                }
            }

            Log.d(TAG, laps.size() + " laps detected");
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
                Log.d(TAG, "LapTime: " + lapTime + ", carry: " + carryOverTime);
                /*
                // Solve for t such that integral over a*t^2 + b*t + c from 0 to t will equal distance to intersection
                double timeToIntersection = solveTimeToIntersection(a, b, initialSpeed, distanceToIntersection);
                lapTime = timeToIntersection + (n-2)/GPS_HZ;
                carryOverTime = 1 - timeToIntersection;*/
            }
        }
        return new Pair<>(lapTime, carryOverTime);
    }

    public static List<LatLng> getFinishLine(List<LocationData> locationData, List<Integer> indexArray, double lineLength) {
        LatLng centerLatLng = LapDetection.getAveragePoint(locationData, indexArray);
        LatLng directionVector = LapDetection.getAverageDirectionVector(locationData, indexArray);

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

    public static int detectLapCountFFT(List<Double> coordinates) {
        int n = coordinates.size();
        int paddedLength = nextPowerOf2(n);
        double[] normalizedData = new double[paddedLength];

        double meanCoordinate = coordinates.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        for (int i = 0; i < n; i++) {
            normalizedData[i] = coordinates.get(i) - meanCoordinate;
        }
        for (int i = n; i < paddedLength; i++) {
            normalizedData[i] = 0;
        }

        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        Complex[] fft = transformer.transform(normalizedData, TransformType.FORWARD);

        // Find the max power / max frequency, which will indicate an estimate of laps done
        double maxPower = 0;
        int maxFrequency = 0;
        for (int i = 1; i < paddedLength / 2; i++) { // Start from 1 to skip the DC component
            double power = fft[i].abs();
            if (power > maxPower) {
                maxPower = power;
                maxFrequency = i;
            }
        }

        double dominantFrequency = maxFrequency * GPS_HZ / paddedLength;
        return (int) Math.round(dominantFrequency * (n));
    }

    public static List<Integer> findClusteredPoints(List<LocationData> locationData, int numOfPoints, double radiusThreshold) {
        int size = locationData.size();
        // The start point will be the first point repeated numOfLaps times
        for (int i = 0; i < size; i++) {
            LatLng currentPoint = locationData.get(i).getCoordinate();
            List<Integer> currentIndexArray = new ArrayList<>();
            currentIndexArray.add(i);
            for (int j = i + 1; j < size; j++) {
                LatLng nextPoint = locationData.get(j).getCoordinate();
                double distance = calculateDistance(currentPoint, nextPoint);

                if (distance <= radiusThreshold) {
                    currentIndexArray.add(j);
                    if (currentIndexArray.size() == numOfPoints) {
                        return currentIndexArray;
                    }
                }
            }
        }
        return new ArrayList<Integer>();
    }

    public static List<Integer> getStartFinishPoints(List<LocationData> locationData) {
        List<Double> longitudes = new ArrayList<>();
        List<Double> latitudes = new ArrayList<>();

        for (LocationData data : locationData) {
            longitudes.add(data.getCoordinate().longitude);
            latitudes.add(data.getCoordinate().latitude);
        }

        // Detect lap count using both longitude and latitude
        int detectLapCountLongitude = detectLapCountFFT(longitudes);
        int detectLapCountLatitude = detectLapCountFFT(latitudes);
        Log.d(TAG, "Detected number of laps LONGITUDE: " + detectLapCountLongitude);
        Log.d(TAG, "Detected number of laps LATITUDE: " + detectLapCountLatitude);

        int estimatedLapCount = Math.min(detectLapCountLatitude, detectLapCountLongitude);
        int threshold = 2; // minimum number of laps expected

        // We identify the start/finish points as the first estimatedLapCount clustered points
        while (estimatedLapCount >= threshold) {
            List<Integer> estimatedStartPoints = findClusteredPoints(locationData, estimatedLapCount, RADIUS_THRESHOLD);
            if (!estimatedStartPoints.isEmpty()) {
                return estimatedStartPoints;
            } else {
                Log.d(TAG, "Could not find start points with " + estimatedLapCount);
                estimatedLapCount--;
            }
        }

        Log.d(TAG, "Could not find start points of lap");
        return null;
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

    public static double solveTimeToIntersection(double a, double b, double c, double distanceToIntersection) {
        double epsilon = 1e-6;
        double left = 0, right = GPS_HZ;
        double t = (left + right) / 2;

        // Bisection loop to narrow down interval
        while (Math.abs(right - left) > epsilon) {
            // Polynomial distance function at t
            double f = (a / 3) * Math.pow(t, 3) + (b / 2) * Math.pow(t, 2) + c * t - distanceToIntersection;

            if (f > 0) {
                right = t;
            } else {
                left = t;
            }

            t = (left + right) / 2;
        }

        return t;
    }

    private static boolean onSegment(LatLng p, LatLng q, LatLng r) {
        return q.latitude <= Math.max(p.latitude, r.latitude) && q.latitude >= Math.min(p.latitude, r.latitude) &&
                q.longitude <= Math.max(p.longitude, r.longitude) && q.longitude >= Math.min(p.longitude, r.longitude);
    }

    public static LatLng getAveragePoint(List<LocationData> locationData, List<Integer> indexArray) {
        if (indexArray.isEmpty()) return null;

        double sumLat = 0, sumLng = 0;
        for (int currPointIndex : indexArray) {
            LatLng currPoint = locationData.get(currPointIndex).getCoordinate();
            sumLat += currPoint.latitude;
            sumLng += currPoint.longitude;
        }
        return new LatLng(sumLat / indexArray.size(), sumLng / indexArray.size());
    }
    public static LatLng getAverageDirectionVector(List<LocationData> locationData, List<Integer> indexArray) {
        if (indexArray.isEmpty()) return null;

        double sumLat = 0, sumLng = 0;
        for (int index : indexArray) {
            int nextIndex = index + 1;
            if (nextIndex >= locationData.size()) break;

            LatLng currPoint = locationData.get(index).getCoordinate();
            LatLng nextPoint = locationData.get(nextIndex).getCoordinate();

            sumLat += currPoint.latitude - nextPoint.latitude;
            sumLng += currPoint.longitude - nextPoint.longitude;
        }

        double magnitude = Math.sqrt(sumLat * sumLat + sumLng * sumLng);
        return new LatLng(sumLat / magnitude, sumLng / magnitude);
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

    private static int nextPowerOf2(int n) {
        int count = 0;
        if (n > 0 && (n & (n - 1)) == 0) {
            return n;
        }
        while (n != 0) {
            n >>= 1;
            count += 1;
        }
        return 1 << count;
    }
}