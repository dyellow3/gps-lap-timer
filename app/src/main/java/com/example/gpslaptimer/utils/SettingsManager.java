package com.example.gpslaptimer.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SettingsManager {
    private static final String TAG = "SettingsManager";
    private static final String SETTINGS_FOLDER = "settings";
    private static final String SETTINGS_FILE = "app_settings.properties";

    private final Properties properties;
    private final File settingsFile;

    public static final Map<String, String> DEFAULT_SETTINGS;
    static {
        Map<String, String> tempMap = new HashMap<>();
        tempMap.put("grid_size", "1.0");
        tempMap.put("direction_tolerance", "15");
        tempMap.put("finish_length", "8");
        DEFAULT_SETTINGS = Collections.unmodifiableMap(tempMap);
    }


    public SettingsManager(Context context) {
        this.properties = new Properties();
        File settingsFolder = new File(context.getFilesDir(), SETTINGS_FOLDER);
        if (!settingsFolder.exists()) {
            settingsFolder.mkdir();
        }
        this.settingsFile = new File(settingsFolder, SETTINGS_FILE);
        loadSettings();
    }

    private void loadSettings() {
        if (!settingsFile.exists()) {
            properties.putAll(DEFAULT_SETTINGS);
            saveSettings();
        } else {
            try (FileInputStream fis = new FileInputStream(settingsFile)) {
                properties.load(fis);
                // Check for missing properties and set default values if needed
                for (Map.Entry<String, String> entry : DEFAULT_SETTINGS.entrySet()) {
                    if (!properties.containsKey(entry.getKey())) {
                        properties.setProperty(entry.getKey(), entry.getValue());
                    }
                }
                saveSettings();
            } catch (IOException e) {
                Log.e(TAG, "Error loading settings", e);
            }
        }
    }

    public void saveSettings() {
        try (FileOutputStream fos = new FileOutputStream(settingsFile)) {
            properties.store(fos, null);
        } catch (IOException e) {
            Log.e(TAG, "Error saving settings", e);
        }
    }

    public void setFloat(String key, float value) {
        properties.setProperty(key, String.valueOf(value));
        saveSettings();
    }

    public float getFloat(String key, float defaultValue) {
        return Float.parseFloat(properties.getProperty(key, String.valueOf(defaultValue)));
    }

}