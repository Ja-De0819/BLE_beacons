package com.example.blebeacons;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

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
        // Retrieve the list of selected Bluetooth devices from the intent
        ArrayList<BTLE_Device> selectedDevices = getIntent().getParcelableArrayListExtra("selectedDevices");
        List<BTLE_Device> selectedDevicesList = new ArrayList<>(selectedDevices); // Convert ArrayList to List

        if (selectedDevicesList != null) {
            // Iterate through the list and display device information
            for (BTLE_Device device : selectedDevicesList) {
                String deviceInfo = "Name: " + device.getName() + "\nAddress: " + device.getAddress() + "\nRSSI: " + device.getRSSI();
                deviceInfoList.add(deviceInfo);
            }
            adapter.notifyDataSetChanged();
        }

        Button btnStartMonitoring = findViewById(R.id.btnStartMonitoring);
        btnStartMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the MonitoringActivity
                Intent intent = new Intent(SelectedDevicesActivity.this, MonitoringActivity.class);
                intent.putExtra("selectedDevices", selectedDevices);
                startActivity(intent);
            }
        });
    }
}
