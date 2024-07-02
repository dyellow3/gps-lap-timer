package com.example.gpslaptimer.ui.settings;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.gpslaptimer.utils.SettingsManager;

import java.util.HashMap;
import java.util.Map;

public class SettingsViewModel extends AndroidViewModel {
    private final SettingsManager settingsManager;
    private final Map<String, MutableLiveData<Float>> settingsMap = new HashMap<>();

    private static final String[] SETTING_KEYS = {"grid_size", "gps_hz", "direction_tolerance", "finish_length"};

    public SettingsViewModel(Application application) {
        super(application);
        settingsManager = new SettingsManager(application);
        loadSettings();
    }

    private void loadSettings() {
        for (String key : SETTING_KEYS) {
            float defaultValue = Float.parseFloat(SettingsManager.DEFAULT_SETTINGS.get(key));
            MutableLiveData<Float> liveData = new MutableLiveData<>(settingsManager.getFloat(key, defaultValue));
            settingsMap.put(key, liveData);
        }
    }

    public LiveData<Float> getSetting(String key) {
        return settingsMap.get(key);
    }

    public void setSetting(String key, float value) {
        settingsMap.get(key).setValue(value);
        settingsManager.setFloat(key, value);
    }
}