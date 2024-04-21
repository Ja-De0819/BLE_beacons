package com.example.blebeacons;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MonitoringActivity extends AppCompatActivity {

    private TextView textViewBeaconInfo;
    private Map<String, Integer> deviceRSSIMap = new HashMap<>();
    private DatabaseReference databaseReference;
    private DatabaseReference databaseReferenceForUserB;
    private FirebaseFirestore firestore;
    private String currentUserId;
    private String userBUid;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 5000; // 5 seconds
    private static final long SCAN_INTERVAL = 20000; // 20 seconds

    private EditText editTextEmail;
    private EditText editTextPassword;
    private Button buttonShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        textViewBeaconInfo = findViewById(R.id.textViewBeaconInfo);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextPassword = findViewById(R.id.editTextPassword);
        buttonShare = findViewById(R.id.buttonShare);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        databaseReference = FirebaseDatabase.getInstance("https://login-register-ce281-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("beacons").child(currentUserId);


        // Start BLE scanning
        startBLEScan();

        // Schedule the scanning process every SCAN_INTERVAL milliseconds
        handler.postDelayed(scanRunnable, SCAN_INTERVAL);

        // Set click listener for the share button
        buttonShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = editTextEmail.getText().toString().trim(); // Get the entered email
                String password = editTextPassword.getText().toString().trim(); // Get the entered password
                shareData(email,password);
            }
        });
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
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE);

                firestore.collection("beacons").document(currentUserId).collection("userBeacons")
                        .document(device.getAddress())
                        .get()
                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                            @Override
                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                if (documentSnapshot.exists()) {

                            // Update RSSI value for the device
                            deviceRSSIMap.put(device.getAddress(),rssi);

                            // Display beacon with the nearest device
                            displayNearestBeacon();
                            // Check if userBUid is not null before calling displayNearestBeaconForB
                            if (userBUid != null) {
                                displayNearestBeaconForB(userBUid);
                            }
                        }
                 }
            });
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
        String nearestDeviceAddress = null;
        for (Map.Entry<String, Integer> entry : deviceRSSIMap.entrySet()) {
            if (entry.getValue() > maxRSSI) {
                maxRSSI = entry.getValue();
                nearestDeviceAddress = entry.getKey();
            }
        }

        if (nearestDeviceAddress != null) {
            firestore.collection("beacons").document(currentUserId).collection("userBeacons")
                    .document(nearestDeviceAddress).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                String beaconName = documentSnapshot.getString("name");
                                String beaconAddress = documentSnapshot.getString("address");
                                int rssi = deviceRSSIMap.getOrDefault(beaconAddress, 0);
                                double distance = Math.pow(10, ((-64 - rssi) / 32.0)); // Calculate distance using the formula
                                String formattedDistance = String.format("%.3f", distance); // Round to three significant digits

                                // Display beacon information
                                textViewBeaconInfo.setText("Name: " + beaconName +
                                        "\nAddress: " + beaconAddress +
                                        "\nRSSI: " + rssi+
                                        "\nDistance: " + formattedDistance+ " cm");

                                // Save beacon information to Firebase database
                                Map<String, Object> beaconInfo = new HashMap<>();
                                beaconInfo.put("name", beaconName);
                                beaconInfo.put("address", beaconAddress);
                                beaconInfo.put("rssi", rssi);
                                beaconInfo.put("distance", formattedDistance+ " cm");
                                databaseReference.setValue(beaconInfo);

                            }
                            else {
                                // If no nearest beacon found, clear the displayed information
                                textViewBeaconInfo.setText("not success");
                            }
                        }
                    });
        }
        else {
            // If no nearest beacon found, clear the displayed information
            textViewBeaconInfo.setText("");
        }
    }

    private void shareData(String email, String password) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, proceed with sharing data
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                userBUid = user.getUid();
                                Toast.makeText(MonitoringActivity.this, userBUid,
                                        Toast.LENGTH_SHORT).show();
                                // Share data with User B using their UID or other identifier
                                // You can implement your data sharing logic here\
                                displayNearestBeaconForB(userBUid);

                            }
                        } else {
                            // If sign in fails, display a message to the user.
                            Toast.makeText(MonitoringActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void displayNearestBeaconForB(String userBUid) {
        int maxRSSI = Integer.MIN_VALUE;
        String nearestDeviceAddress = null;
        for (Map.Entry<String, Integer> entry : deviceRSSIMap.entrySet()) {
            if (entry.getValue() > maxRSSI) {
                maxRSSI = entry.getValue();
                nearestDeviceAddress = entry.getKey();
            }
        }

        if (nearestDeviceAddress != null) {
            firestore.collection("beacons").document(currentUserId).collection("userBeacons")
                    .document(nearestDeviceAddress).get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                String beaconName = documentSnapshot.getString("name");
                                String beaconAddress = documentSnapshot.getString("address");
                                int rssi = deviceRSSIMap.getOrDefault(beaconAddress, 0);
                                double distance = Math.pow(10, ((-64 - rssi) / 32.0)); // Calculate distance using the formula
                                String formattedDistance = String.format("%.3f", distance); // Round to three significant digits
                                databaseReferenceForUserB = FirebaseDatabase.getInstance("https://login-register-ce281-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                        .getReference("Monitor").child(userBUid);

                                // Save beacon information to Firebase database
                                Map<String, Object> beaconInfo = new HashMap<>();
                                beaconInfo.put("name", beaconName);
                                beaconInfo.put("address", beaconAddress);
                                beaconInfo.put("rssi", rssi);
                                beaconInfo.put("distance", formattedDistance + " cm");
                                databaseReferenceForUserB.setValue(beaconInfo);

                            }
                            else {
                                // If no nearest beacon found, clear the displayed information
                                textViewBeaconInfo.setText("not success");
                            }
                        }
                    });
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
