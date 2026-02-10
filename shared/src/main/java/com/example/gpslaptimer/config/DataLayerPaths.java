package com.example.gpslaptimer.config;

/**
 * Shared constants for the Wearable Data Layer API.
 * Used by both :wear (sender) and :app (receiver).
 */
public final class DataLayerPaths {

    private DataLayerPaths() {
        // No instances
    }

    /** Base path for track data items. Each track appends "/<timestamp>". */
    public static final String TRACK_DATA_PATH = "/track";

    /** Asset key for the CSV byte data. */
    public static final String KEY_CSV_ASSET = "csv_data";

    /** Metadata key for the original file name. */
    public static final String KEY_FILE_NAME = "file_name";

    /** Metadata key for the send timestamp (millis). */
    public static final String KEY_TIMESTAMP = "timestamp";
}
