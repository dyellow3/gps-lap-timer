package com.example.gpslaptimer.wear;

import android.content.Context;
import android.util.Log;

import com.example.gpslaptimer.config.DataLayerPaths;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class WearDataSyncManager {

    private static final String TAG = "WearDataSyncManager";

    public interface SyncCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    /**
     * Reads csvFile into memory and sends it to the phone via the Data Layer API.
     * The callback fires on the main thread (via Play Services Task API).
     *
     * @param context  any Context (Activity, Service, Application)
     * @param csvFile  the CSV file to send
     * @param fileName the display name for the file
     * @param callback result callback
     */
    public static void sendTrackFile(Context context, File csvFile, String fileName,
                                     SyncCallback callback) {
        if (!csvFile.exists() || csvFile.length() == 0) {
            Log.w(TAG, "CSV file missing or empty: " + csvFile.getAbsolutePath());
            callback.onFailure(new IOException("CSV file is missing or empty"));
            return;
        }

        byte[] csvBytes;
        try {
            csvBytes = readFileBytes(csvFile);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read CSV file", e);
            callback.onFailure(e);
            return;
        }

        long timestamp = System.currentTimeMillis();
        String path = DataLayerPaths.TRACK_DATA_PATH + "/" + timestamp;

        Asset csvAsset = Asset.createFromBytes(csvBytes);

        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(path);
        putDataMapRequest.getDataMap().putAsset(DataLayerPaths.KEY_CSV_ASSET, csvAsset);
        putDataMapRequest.getDataMap().putString(DataLayerPaths.KEY_FILE_NAME, fileName);
        putDataMapRequest.getDataMap().putLong(DataLayerPaths.KEY_TIMESTAMP, timestamp);
        putDataMapRequest.setUrgent();

        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        DataClient dataClient = Wearable.getDataClient(context);
        dataClient.putDataItem(request)
                .addOnSuccessListener(dataItem -> {
                    Log.d(TAG, "Track data queued for sync: " + path);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to queue track data", e);
                    callback.onFailure(e);
                });
    }

    private static byte[] readFileBytes(File file) throws IOException {
        byte[] bytes = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int offset = 0;
            while (offset < bytes.length) {
                int read = fis.read(bytes, offset, bytes.length - offset);
                if (read == -1) {
                    break;
                }
                offset += read;
            }
        }
        return bytes;
    }
}
