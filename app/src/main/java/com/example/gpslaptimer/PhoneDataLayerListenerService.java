package com.example.gpslaptimer;

import android.util.Log;

import com.example.gpslaptimer.config.DataLayerPaths;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Receives track CSV data sent from the Wear OS watch via the Data Layer API.
 * Play Services auto-starts this service when a data item matching /track/* arrives,
 * even if the phone app is not running.
 */
public class PhoneDataLayerListenerService extends WearableListenerService {

    private static final String TAG = "PhoneDataLayerListener";
    private static final int BUFFER_SIZE = 4096;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataItem item = event.getDataItem();
                String path = item.getUri().getPath();
                if (path != null && path.startsWith(DataLayerPaths.TRACK_DATA_PATH + "/")) {
                    handleTrackData(item);
                }
            }
        }
    }

    private void handleTrackData(DataItem dataItem) {
        DataMap dataMap = DataMapItem.fromDataItem(dataItem).getDataMap();
        Asset csvAsset = dataMap.getAsset(DataLayerPaths.KEY_CSV_ASSET);
        String fileName = dataMap.getString(DataLayerPaths.KEY_FILE_NAME);

        if (csvAsset == null) {
            Log.w(TAG, "Received track data with no CSV asset");
            return;
        }

        // De-duplicate: skip if this file was already saved from a previous sync
        String saveName = (fileName != null && !fileName.isEmpty())
                ? "W-" + fileName
                : "W-unknown";
        DataClient dataClient = Wearable.getDataClient(this);
        File directory = getExternalFilesDir(null);

        if (directory != null && new File(directory, saveName).exists()) {
            Log.d(TAG, "File already exists, skipping duplicate: " + saveName);
            deleteDataItem(dataClient, dataItem);
            return;
        }

        InputStream inputStream;
        try {
            inputStream = Tasks.await(dataClient.getFdForAsset(csvAsset)).getInputStream();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get InputStream for asset", e);
            return; // Don't delete — will retry on next sync
        }

        if (inputStream == null) {
            Log.w(TAG, "InputStream for asset is null");
            return;
        }

        try {
            saveCsvToFile(inputStream, saveName);
            deleteDataItem(dataClient, dataItem);
            Log.d(TAG, "Track file saved and data item cleaned up: " + saveName);
        } catch (IOException e) {
            Log.e(TAG, "Failed to save CSV file, will retry on next sync", e);
            // Don't delete — data item remains for retry
        } finally {
            try {
                inputStream.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void saveCsvToFile(InputStream inputStream, String baseName) throws IOException {
        File directory = getExternalFilesDir(null);
        if (directory == null) {
            throw new IOException("External files directory is null");
        }

        String uniqueName = getUniqueFileName(directory, baseName);
        File outputFile = new File(directory, uniqueName);

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
        }

        Log.d(TAG, "CSV saved: " + outputFile.getAbsolutePath());
    }

    private String getUniqueFileName(File directory, String baseName) {
        File file = new File(directory, baseName);
        int index = 1;
        while (file.exists()) {
            String newName = baseName + "(" + index + ")";
            file = new File(directory, newName);
            index++;
        }
        return file.getName();
    }

    private void deleteDataItem(DataClient dataClient, DataItem dataItem) {
        try {
            Tasks.await(dataClient.deleteDataItems(dataItem.getUri()));
        } catch (Exception e) {
            Log.w(TAG, "Failed to delete data item after saving", e);
        }
    }
}
