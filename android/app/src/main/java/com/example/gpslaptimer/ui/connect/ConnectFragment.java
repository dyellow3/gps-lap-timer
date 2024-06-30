package com.example.gpslaptimer.ui.connect;
import com.example.gpslaptimer.adapters.BluetoothDeviceAdapter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.gpslaptimer.R;
import com.example.gpslaptimer.ui.add.AddViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ConnectFragment extends Fragment {

    public ConnectFragment() { }

    private final String TAG = "ConnectFragment";
    private Button scanButton;
    private RecyclerView recyclerView;
    private ActivityResultLauncher<Intent> enableBluetoothLauncher;
    private ActivityResultLauncher<String[]> permissionLauncher;
    BluetoothAdapter bluetoothAdapter;
    BluetoothManager bluetoothManager;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothDevice bluetoothDevice = null;
    private BluetoothDeviceAdapter adapter;
    private ConnectViewModel connectViewModel;
    private AddViewModel addViewModel;
    private List<BluetoothDeviceModel> deviceModels = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager != null ? bluetoothManager.getAdapter() : null;

        enableBluetoothLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> { }
        );

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                perms -> {
                    boolean canEnableBluetooth = true;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        canEnableBluetooth = Boolean.TRUE.equals(perms.get(android.Manifest.permission.BLUETOOTH_CONNECT));
                    }
                    if (canEnableBluetooth && !bluetoothAdapter.isEnabled()) {
                        enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_connect, container, false);
        connectViewModel = new ViewModelProvider(requireActivity()).get(ConnectViewModel.class);
        addViewModel = new ViewModelProvider(requireActivity()).get(AddViewModel.class);

        scanButton = rootView.findViewById(R.id.button_scan);
        recyclerView = rootView.findViewById(R.id.recyclerViewDevices);
        adapter = new BluetoothDeviceAdapter(new ArrayList<>(), this::onConnectClick);
        recyclerView.setAdapter(adapter);

        scanButton.setOnClickListener(v -> {
            bluetoothManager = (BluetoothManager) requireContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if(bluetoothManager != null) {
                bluetoothAdapter = bluetoothManager.getAdapter();
            } else {
                bluetoothAdapter = null;
            }

            deviceModels.clear();

            // Request permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (requireContext().checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if(bluetoothAdapter == null) {
                        Log.d(TAG, "Bluetooth not available");
                    } else if (!bluetoothAdapter.isEnabled()){
                        permissionLauncher.launch(new String[]{
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT
                        });
                    } else {
                        getBTPairedDevices();
                    }
                }
                else {
                    permissionLauncher.launch(new String[]{
                            Manifest.permission.BLUETOOTH_SCAN,
                            Manifest.permission.BLUETOOTH_CONNECT
                    });
                }
            }
        });

        return rootView;
    }

    @SuppressLint("MissingPermission")
    private void onConnectClick(BluetoothDeviceModel bluetoothDeviceModel) {
        // Show dialog for connecting to the device
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_connect_device, null);
        builder.setView(dialogView);

        TextView deviceNameTextView = dialogView.findViewById(R.id.deviceName);
        TextView deviceAddressTextView = dialogView.findViewById(R.id.deviceAddress);
        Button connectButton = dialogView.findViewById(R.id.buttonConnect);
        Button cancelButton = dialogView.findViewById(R.id.buttonCancel);

        deviceNameTextView.setText(bluetoothDeviceModel.getName());
        deviceAddressTextView.setText(bluetoothDeviceModel.getHardwareAddress());

        AlertDialog dialog = builder.create();
        dialog.show();

        connectButton.setOnClickListener(v -> {
            Log.d(TAG, "Attempting to connect to: " + bluetoothDeviceModel.name + " " + bluetoothDeviceModel.hardwareAddress);

            if(bluetoothAdapter == null) {
                Log.d(TAG, "Bluetooth Adapter is null");
                return;
            }

            connectButton.setEnabled(false);
            cancelButton.setEnabled(false);

            bluetoothDevice = bluetoothAdapter.getRemoteDevice(bluetoothDeviceModel.getHardwareAddress());
            connectViewModel.setBluetoothDevice(bluetoothDevice);
            connectViewModel.connectToDevice();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        // Connection result observation
        connectViewModel.getConnectionStatus().observe(getViewLifecycleOwner(), connectionStatus -> {
            if (connectionStatus != null && connectionStatus) {
                dialog.dismiss();
                connectViewModel.setPreviouslyConnected(true);
            } else {
                // On failure
                connectButton.setEnabled(true);
                cancelButton.setEnabled(true);
            }
        });

        // Reconnect / disconnect observation
        addViewModel.getIsRunning().observe(getViewLifecycleOwner(), isRunning -> {
           if(isRunning && Boolean.TRUE.equals(connectViewModel.getPreviouslyConnected().getValue())) {
               Log.d(TAG, "Starting reconnect");
               connectViewModel.startReconnect();
           } else {
               Log.d(TAG, "Stopping reconnect");
               connectViewModel.stopReconnect();
           }
        });
    }

    @SuppressLint("MissingPermission")
    private void getBTPairedDevices() {
        if (!bluetoothAdapter.isEnabled()) {
            enableBluetoothLauncher.launch(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE));
            return;
        }

        pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices != null) {
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                if (deviceName != null) {
                    String deviceHardwareAddress = device.getAddress();
                    Log.d(TAG, deviceName + "\n");
                    Log.d(TAG, deviceHardwareAddress + "\n");
                    deviceModels.add(new BluetoothDeviceModel(deviceName, deviceHardwareAddress));
                }
            }
        }
        adapter.setDeviceList(deviceModels);
    }

    public static class BluetoothDeviceModel {
        private String name;
        private String hardwareAddress;

        public BluetoothDeviceModel(String name, String hardwareAddress) {
            this.name = name;
            this.hardwareAddress = hardwareAddress;
        }

        public String getName() {
            return name;
        }

        public String getHardwareAddress() {
            return hardwareAddress;
        }
    }


}