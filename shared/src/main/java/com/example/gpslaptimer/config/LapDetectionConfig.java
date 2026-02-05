package com.example.gpslaptimer.config;

/**
 * Plain configuration object for lap detection parameters.
 * Replaces the SettingsViewModel dependency so LapDetection
 * can run without Android UI classes.
 */
public class LapDetectionConfig {
    private final float finishLength;
    private final float gridSize;
    private final float directionTolerance;

    public LapDetectionConfig(float finishLength, float gridSize, float directionTolerance) {
        this.finishLength = finishLength;
        this.gridSize = gridSize;
        this.directionTolerance = directionTolerance;
    }

    public float getFinishLength() {
        return finishLength;
    }

    public float getGridSize() {
        return gridSize;
    }

    public float getDirectionTolerance() {
        return directionTolerance;
    }
}
