package com.example.gpslaptimer;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.example.gpslaptimer.databinding.ActivityMainBinding;
import com.example.gpslaptimer.ui.add.AddFragment;
import com.example.gpslaptimer.ui.history.HistoryFragment;
import com.example.gpslaptimer.ui.map.MapFragment;
import com.example.gpslaptimer.ui.settings.SettingsFragment;
import com.example.gpslaptimer.ui.settings.SettingsViewModel;

import org.apache.commons.math3.analysis.function.Add;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        replaceFragment(new AddFragment());

        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
            switch(item.getItemId()) {
                case R.id.add:
                    Log.d(TAG, "Loading AddFragment");
                    replaceFragment(new AddFragment());
                    break;
                case R.id.history:
                    Log.d(TAG, "Loading HistoryFragment");
                    replaceFragment(new HistoryFragment());
                    break;
                case R.id.settings:
                    Log.d(TAG, "Loading SettingsFragment");
                    replaceFragment(new SettingsFragment());
            }
            return true;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void onMapFragmentRequest() {
        replaceFragment(new MapFragment());
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.mainFrameLayout, fragment);
        fragmentTransaction.commit();
    }

}