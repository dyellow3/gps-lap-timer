package com.example.gpslaptimer.ui.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.gpslaptimer.R;
import com.google.android.material.slider.Slider;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {
    private SettingsViewModel settingsViewModel;

    private Map<String, Slider> sliders = new HashMap<>();
    private Map<String, TextView> textViews = new HashMap<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_settings, container, false);

        settingsViewModel = new ViewModelProvider(requireActivity()).get(SettingsViewModel.class);

        initializeViews(rootView);
        observeSettings();
        setupSliderListeners();

        return rootView;
    }

    private void initializeViews(View rootView) {
        sliders.put("grid_size", rootView.findViewById(R.id.sliderGridSize));
        sliders.put("direction_tolerance", rootView.findViewById(R.id.sliderDirectionTolerance));
        sliders.put("finish_length", rootView.findViewById(R.id.sliderFinishLength));

        textViews.put("grid_size", rootView.findViewById(R.id.textViewGridSize));
        textViews.put("direction_tolerance", rootView.findViewById(R.id.textViewDirectionTolerance));
        textViews.put("finish_length", rootView.findViewById(R.id.textViewFinishLength));
    }

    private void observeSettings() {
        for (String key : sliders.keySet()) {
            settingsViewModel.getSetting(key).observe(getViewLifecycleOwner(), value -> {
                sliders.get(key).setValue(value);
                updateTextView(key, value);
            });
        }
    }

    private void setupSliderListeners() {
        for (Map.Entry<String, Slider> entry : sliders.entrySet()) {
            String key = entry.getKey();
            Slider slider = entry.getValue();
            slider.addOnChangeListener((s, value, fromUser) -> {
                if (fromUser) {
                    settingsViewModel.setSetting(key, value);
                    updateTextView(key, value);
                }
            });
        }
    }

    private void updateTextView(String key, float value) {
        String text;
        switch (key) {
            case "grid_size":
                text = String.format("Grid Size: %.2f meters", value);
                break;
            case "direction_tolerance":
                text = String.format("Direction Tolerance: %.2f degrees", value);
                break;
            case "finish_length":
                text = String.format("Finish line length: %.2f meters", value);
                break;
            default:
                throw new IllegalArgumentException("Unexpected key: " + key);
        }
        textViews.get(key).setText(text);
    }
}
