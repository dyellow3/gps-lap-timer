package com.example.gpslaptimer.ui.connect;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConnectViewModel extends ViewModel {
    private static final String TAG = "AddViewModel";
    private static final UUID SERIAL_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final MutableLiveData<BluetoothSocket> bluetoothSocket = new MutableLiveData<BluetoothSocket>();
    private final MutableLiveData<Boolean> connectionStatus = new MutableLiveData<>(false);
    private final MutableLiveData<String> connectStatus = new MutableLiveData<>("Not connected");
    private final MutableLiveData<Boolean> previouslyConnected = new MutableLiveData<>(false);

    private BluetoothDevice bluetoothDevice = null;
    private Thread connectionThread = null;
    private final Handler handler = new Handler();
    private final Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            if(bluetoothDevice != null && Boolean.FALSE.equals(connectionStatus.getValue())) {
                connectToDevice();
            }
            handler.postDelayed(this, TimeUnit.SECONDS.toMillis(5)); // Attempt reconnection every 5 seconds
        }
    };

    public LiveData<BluetoothSocket> getBluetoothSocket() {
        return bluetoothSocket;
    }

    public LiveData<Boolean> getConnectionStatus() {
        return connectionStatus;
    }

    public LiveData<String> getConnectStatus() {
        return connectStatus;
    }

    public LiveData<Boolean> getPreviouslyConnected() {
        return previouslyConnected;
    }
    public void setPreviouslyConnected(boolean isConnected) {
        previouslyConnected.setValue(isConnected);
    }

    public void setBluetoothDevice(BluetoothDevice device) {
        disconnect();
        this.bluetoothDevice = device;
    }

    public void startReconnect() {
        handler.postDelayed(reconnectRunnable, TimeUnit.SECONDS.toMillis(5));
    }
    public void stopReconnect() {
        handler.removeCallbacks(reconnectRunnable);
    }

    @SuppressLint("MissingPermission")
    public void connectToDevice() {
        if (bluetoothDevice == null) {
            Log.e("HomeViewModel", "Bluetooth device not set");
            connectionStatus.postValue(false);
            return;
        }

        connectionThread = new Thread(() -> {
            BluetoothSocket socket = null;
            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(SERIAL_UUID);
                socket.connect();

                bluetoothSocket.postValue(socket);
                connectionStatus.postValue(true);
                connectStatus.postValue("Connected to: " + bluetoothDevice.getName());
                startReadingMessages(socket);
            } catch (IOException e) {
                Log.e(TAG, "Connection failed", e);
                disconnect();
            } finally {
                if(Boolean.FALSE.equals(connectionStatus.getValue())) {
                    connectionStatus.postValue(false);
                    connectStatus.postValue("Not connected");
                }
            }
        });
        connectionThread.start();
    }

    public void disconnect() {
        BluetoothSocket socket = bluetoothSocket.getValue();
        if (socket != null) {
            try {
                socket.close();
                bluetoothSocket.postValue(null);
                connectionStatus.postValue(false);
                if (connectionThread != null && connectionThread.isAlive()) {
                    connectionThread.interrupt();
                }
                connectStatus.postValue("Not connected");
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
    }

    private Thread readThread;
    private final MutableLiveData<String> receivedMessage = new MutableLiveData<>();
    public LiveData<String> getReceivedMessage() {
        return receivedMessage;
    }
    private void startReadingMessages(BluetoothSocket socket) {
        readThread = new Thread(() -> {
            try {
                InputStream inputStream = socket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytesRead;
                StringBuilder receivedMessageBuilder = new StringBuilder();

                while (true) {
                    bytesRead = inputStream.read(buffer);
                    if (bytesRead > 0) {
                        for (int i = 0; i < bytesRead; i++) {
                            char c = (char) buffer[i];
                            if (c == '\n') {
                                String message = receivedMessageBuilder.toString().trim();
                                receivedMessage.postValue(message);
                                receivedMessageBuilder.setLength(0);
                            } else {
                                receivedMessageBuilder.append(c);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Error reading from socket", e);
                connectionStatus.postValue(false);
                connectStatus.postValue("Not connected");
                disconnect();
            }
        });
        readThread.start();
    }
}
