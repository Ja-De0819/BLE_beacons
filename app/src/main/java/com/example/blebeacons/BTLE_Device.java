package com.example.blebeacons;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Kelvin on 5/8/16.
 */
public class BTLE_Device {

    private BluetoothDevice bluetoothDevice;
    private int rssi;
    private String address;
    private String name;

    public BTLE_Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    // Constructor to create BTLE_Device object with name, address, and RSSI
    public BTLE_Device(String name, String address, int rssi) {
        this.name = name;
        this.address = address;
        this.rssi = rssi;
    }

    public String getAddress() {
        return bluetoothDevice.getAddress();
    }

    public String getName() {
        return bluetoothDevice.getName();
    }

    public int getRSSI() { return rssi;}

    public void setName(String name) { this.name = name; }

    public void setRSSI(int rssi) {
        this.rssi = rssi;
    }

    /**
     * Gets the underlying BluetoothDevice object.
     * @return The BluetoothDevice object.
     */

}