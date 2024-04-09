package com.example.blebeacons;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class SelectedDevicesActivity extends AppCompatActivity {

    private ListView listView;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> deviceInfoList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_devices);

        listView = findViewById(R.id.listViewSelectedDevices);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, deviceInfoList);
        listView.setAdapter(adapter);

        // Retrieve the list of selected Bluetooth devices from the intent
        ArrayList<BTLE_Device> selectedDevices = getIntent().getParcelableArrayListExtra("selectedDevices");
        if (selectedDevices != null) {
            // Iterate through the list and display device information
            for (BTLE_Device device : selectedDevices) {
                String deviceInfo = "Name: " + device.getName() + "\nAddress: " + device.getAddress()+ device.getAddress() + "\nRSSI: " + device.getRSSI();;
                deviceInfoList.add(deviceInfo);
            }
            adapter.notifyDataSetChanged();
        }
    }
}
