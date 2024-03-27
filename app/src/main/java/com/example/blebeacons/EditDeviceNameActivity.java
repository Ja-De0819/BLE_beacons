package com.example.blebeacons;

import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class EditDeviceNameActivity extends AppCompatActivity {
        public static final String EXTRA_DEVICE_NAME = "device_name";
        public static final String EXTRA_BLUETOOTH_DEVICE = "bluetooth_device";
        public static final String EXTRA_DEVICE_ADDRESS = "device_address";


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_edit_device_name);

           final EditText editTextName = findViewById(R.id.edit_text_name);
           String deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
           editTextName.setText(deviceName);

            final BluetoothDevice bluetoothDevice = getIntent().getParcelableExtra(EXTRA_BLUETOOTH_DEVICE);

            String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);
            TextView deviceAddressTextView = findViewById(R.id.device_address);
            deviceAddressTextView.setText(deviceAddress);


            Button btnSave = findViewById(R.id.btn_save);
            btnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String newName = editTextName.getText().toString();
                    Intent intent = new Intent(EditDeviceNameActivity.this, ConnectedDevicesActivity.class);
                    intent.putExtra(ConnectedDevicesActivity.EXTRA_NEW_NAME, newName); // Use the correct key
                    intent.putExtra(ConnectedDevicesActivity.EXTRA_EDITED_DEVICE, bluetoothDevice); // Use the correct key
                    startActivity(intent);
                    finish();
                }
            });

        }
    }

