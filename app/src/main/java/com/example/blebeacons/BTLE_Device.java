package com.example.blebeacons;

import android.bluetooth.BluetoothDevice;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Kelvin on 5/8/16.
 */
public class BTLE_Device implements Parcelable {

    private BluetoothDevice bluetoothDevice;
    private int rssi;
    private String address;
    private String name;

    public BTLE_Device(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }

    protected BTLE_Device(Parcel in) {
        bluetoothDevice = in.readParcelable(BluetoothDevice.class.getClassLoader());
        rssi = in.readInt();
    }

    public static final Creator<BTLE_Device> CREATOR = new Creator<BTLE_Device>() {
        @Override
        public BTLE_Device createFromParcel(Parcel in) {
            return new BTLE_Device(in);
        }

        @Override
        public BTLE_Device[] newArray(int size) {
            return new BTLE_Device[size];
        }
    };

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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(bluetoothDevice, flags);
        dest.writeInt(rssi);
    }

    /**
     * Gets the underlying BluetoothDevice object.
     * @return The BluetoothDevice object.
     */

}