package com.example.blebeacons;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity {

    private TextView textViewBeaconInfo;
    private Map<String, Integer> deviceRSSIMap = new HashMap<>();
    private List<BTLE_Device> selectedDevices;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 5000; // 5 seconds
    private static final long SCAN_INTERVAL = 20000; // 20 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        textViewBeaconInfo = findViewById(R.id.textViewBeaconInfo);

        // Retrieve the list of selected Bluetooth devices from the intent
        selectedDevices = getIntent().getParcelableArrayListExtra("selectedDevices");

        // Start BLE scanning
        startBLEScan();

        // Schedule the scanning process every SCAN_INTERVAL milliseconds
        handler.postDelayed(scanRunnable, SCAN_INTERVAL);
    }

    private void startBLEScan() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            // Bluetooth is not available or not enabled
            return;
        }

        // Register broadcast receiver for BLE scan results
        registerReceiver(bluetoothReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        // Start BLE scan
        bluetoothAdapter.startDiscovery();

        // Schedule the stop of scanning after SCAN_PERIOD milliseconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                bluetoothAdapter.cancelDiscovery();
                showToast("Scanning stopped");
            }
        }, SCAN_PERIOD);

// Schedule the showing of "Scanning started" toast after the scan interval
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showToast("Scanning started");
            }
        }, SCAN_INTERVAL);

    }

    private final BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // A new BLE device is found
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                // Check if the found device is in the selected devices list
                for (BTLE_Device selectedDevice : selectedDevices) {
                    if (selectedDevice.getAddress().equals(device.getAddress())) {
                        // Update RSSI value for the device
                        deviceRSSIMap.put(device.getAddress(), rssi);

                        // Display beacon with the nearest device
                        displayNearestBeacon();
                        break; // Exit loop since the device is found in the selected devices list
                    }
                }
            }
        }
    };

    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            // Start BLE scanning
            startBLEScan();
            // Schedule the next scanning process after SCAN_INTERVAL milliseconds
            handler.postDelayed(this, SCAN_INTERVAL);
        }
    };

    private void displayNearestBeacon() {
        int maxRSSI = Integer.MIN_VALUE;
        String nearestDevice = null;
        for (Map.Entry<String, Integer> entry : deviceRSSIMap.entrySet()) {
            if (entry.getValue() > maxRSSI) {
                maxRSSI = entry.getValue();
                nearestDevice = entry.getKey();
            }
        }

        if (nearestDevice != null) {
            // Display beacon information
            textViewBeaconInfo.setText("Nearest Device Address: " + nearestDevice + "\nRSSI: " + maxRSSI);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop BLE scanning and unregister receiver
        BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        unregisterReceiver(bluetoothReceiver);
    }
}
