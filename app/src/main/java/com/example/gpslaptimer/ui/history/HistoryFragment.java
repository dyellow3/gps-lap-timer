package com.example.gpslaptimer.ui.history;
import com.example.gpslaptimer.adapters.HistoryAdapter;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.example.gpslaptimer.MainActivity;
import com.example.gpslaptimer.R;
import com.example.gpslaptimer.ui.map.MapViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class HistoryFragment extends Fragment {
    private final String TAG = "HistoryFragment";

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    MapViewModel mapViewModel;
    private List<String> lapFiles;

    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_history, container, false);

        mapViewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        recyclerView = rootView.findViewById(R.id.recyclerViewLaps);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        lapFiles = readLapFiles();
        adapter = new HistoryAdapter(lapFiles, new HistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String lapFileName) {
                mapViewModel.setFileName(lapFileName);
                ((MainActivity) getActivity()).onMapFragmentRequest();
            }

            @Override
            public void onDeleteClick(String lapFileName, int position) {
                // Show dialog for confirmation
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                LayoutInflater inflater = requireActivity().getLayoutInflater();
                View dialogView = inflater.inflate(R.layout.dialog_confirm, null);
                builder.setView(dialogView);

                Button confirmButton = dialogView.findViewById(R.id.buttonConfirm);
                Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

                AlertDialog dialog = builder.create();
                dialog.show();

                confirmButton.setOnClickListener(v -> {
                    deleteFile(lapFileName);
                    lapFiles.remove(position);
                    adapter.notifyItemRemoved(position);
                    dialog.dismiss();
                });

                cancelButton.setOnClickListener(v -> dialog.dismiss());
            }
        });
        recyclerView.setAdapter(adapter);

        return rootView;
    }

    private List<String> readLapFiles() {
        List<String> lapFiles = new ArrayList<>();
        File directory = getContext().getExternalFilesDir(null);
        Log.d(TAG, "Files directory: " + directory.getAbsolutePath());
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().contains("-")) {
                    Log.d(TAG, file.getName());
                    lapFiles.add(file.getName());
                }
            }
        }
        return lapFiles;
    }

    private void deleteFile(String lapFileName) {
        File directory = getContext().getExternalFilesDir(null);
        File file = new File(directory, lapFileName);
        if (file.exists() && file.delete()) {
            Log.d(TAG, "File deleted: " + lapFileName);
        } else {
            Log.d(TAG, "Failed to delete file: " + lapFileName);
        }
    }
}