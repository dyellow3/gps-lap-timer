package com.example.gpslaptimer.models;

public class BluetoothDevice {
    private String name;
    private String hardwareAddress;

    public BluetoothDevice(String name, String hardwareAddress) {
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
