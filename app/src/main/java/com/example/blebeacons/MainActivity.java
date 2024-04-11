package com.example.blebeacons;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int BTLE_SERVICES = 2;
    public static final int EDIT_DEVICE_NAME_REQUEST = 2;


    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private List<BTLE_Device> selectedDevices = new ArrayList<>();
    private ListAdapter_BTLE_Devices adapter;
    private ListView listView;

    private FirebaseAuth mAuth;
    private String currentUserId;
    private TextView greetingTextView;

    private Button btn_Scan;
    private Button btn_Logout;

    private BroadcastReceiver_BTState mBTStateUpdateReceiver;
    private Scanner_BTLE mBTLeScanner;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Use this check to determine whether BLE is supported on the device. Then
        // you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Utils.toast(getApplicationContext(), "BLE not supported");
            finish();
        }

        mBTStateUpdateReceiver = new BroadcastReceiver_BTState(getApplicationContext());
        mBTLeScanner = new Scanner_BTLE(this, 10000, -75);

        mBTDevicesHashMap = new HashMap<>();
        mBTDevicesArrayList = new ArrayList<>();
        selectedDevices = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        greetingTextView = findViewById(R.id.textViewGreeting);
        updateGreeting();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        btn_Scan = findViewById(R.id.btn_scan);
        btn_Logout = findViewById(R.id.btn_logout);
        Button btn_proceed = findViewById(R.id.btn_proceed);

        ((ScrollView) findViewById(R.id.scrollView)).addView(listView);
        findViewById(R.id.btn_scan).setOnClickListener(this);
        findViewById(R.id.btn_proceed ).setOnClickListener(this);
        btn_Logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add code to log out and navigate back to the login page
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivity.this, Login.class);
                startActivity(intent);
                finish();
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android M Permission check
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(authStateListener);
        registerReceiver(mBTStateUpdateReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

        stopScan();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            mAuth.removeAuthStateListener(authStateListener);
        }
        unregisterReceiver(mBTStateUpdateReceiver);
        stopScan();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EDIT_DEVICE_NAME_REQUEST && resultCode == RESULT_OK) {
            String newName = data.getStringExtra("newName");

            // Ensure that the editedDevice is not null before accessing its properties
            BTLE_Device editedDevice = data.getParcelableExtra("editedDevice");
            if (editedDevice != null) {
                // Update the name of the edited device in the selectedDevices list
                for (BTLE_Device device : selectedDevices) {
                    if (device.getAddress().equals(editedDevice.getAddress())) {
                        // Found the edited device, update its name
                        device.setName(newName);
                        // Optionally, update the UI to reflect the new name
                        adapter.notifyDataSetChanged(); // Assuming you have an adapter for the list view
                        break; // Exit loop since device is found
                    }
                }
            } else {
                // Handle the case where editedDevice is null
                Utils.toast(getApplicationContext(), "Edited device is null");
                Log.e(TAG, "Edited device is null");
            }
        }
    }

    /**
     * Called when an item in the ListView is clicked.
     */

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        BTLE_Device selectedDevice = mBTDevicesArrayList.get(position);
        if (selectedDevice != null) {
            // Save the selected device to Firestore
            saveSelectedDeviceToFirestore(selectedDevice);
            // Add the selected device to the list of selected devices
            selectedDevices.add(selectedDevice);
            // Notify the user
            Toast.makeText(this, "Added device to the list", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Selected device is null", Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * Called when the scan button is clicked.
     *
     * @param v The view that was clicked
     */

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_scan:
                Utils.toast(getApplicationContext(), "Scan Button Pressed");
                if (!mBTLeScanner.isScanning()) {
                    startScan();
                } else {
                    stopScan();
                }
                break;

            case R.id.btn_proceed:
                // Check if any devices are selected
                if (!selectedDevices.isEmpty()) {
                    // Navigate to the SelectedDevicesActivity to display selected devices
                    goToSelectedDevicesActivity();
                } else {
                    Utils.toast(getApplicationContext(), "Please select at least one device.");
                }
                break;

            default:
                break;
        }
    }


    /**
     * Adds a device to the ArrayList and Hashmap that the ListAdapter is keeping track of.
     *
     * @param device the BluetoothDevice to be added
     * @param rssi   the rssi of the BluetoothDevice
     */
    public void addDevice(BluetoothDevice device, int rssi) {

        String address = device.getAddress();
        if (!mBTDevicesHashMap.containsKey(address)) {
            BTLE_Device btleDevice = new BTLE_Device(device);
            btleDevice.setRSSI(rssi);

            mBTDevicesHashMap.put(address, btleDevice);
            mBTDevicesArrayList.add(btleDevice);
        } else {
            mBTDevicesHashMap.get(address).setRSSI(rssi);
        }

        adapter.notifyDataSetChanged();
    }

    private void saveSelectedDeviceToFirestore(BTLE_Device selectedDevice) {
        mAuth = FirebaseAuth.getInstance();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        // Assume you have a collection named "selectedDevices" in Firestore
        CollectionReference beaconsRef = db.collection("beacons").document(currentUserId).collection("userBeacons");
        // Create a document for the selected device
        HashMap<String, Object> deviceData = new HashMap<>();
        deviceData.put("address", selectedDevice.getAddress());
        deviceData.put("name", selectedDevice.getName());
        deviceData.put("rssi", selectedDevice.getRSSI());
        deviceData.put("userId", currentUserId); // Include the user's ID in the beacon data
        // Add other fields as needed

        beaconsRef.document(selectedDevice.getAddress()).set(deviceData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Selected device added to Firestore");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding selected device to Firestore", e);
                    }
                });
    }



    /**
     * Clears the ArrayList and Hashmap the ListAdapter is keeping track of.
     * Starts Scanner_BTLE.
     * Changes the scan button text.
     */
    public void startScan() {
        btn_Scan.setText("Scanning...");

        mBTDevicesArrayList.clear();
        mBTDevicesHashMap.clear();

        adapter.notifyDataSetChanged();

        mBTLeScanner.start();
    }

    /**
     * Stops Scanner_BTLE
     * Changes the scan button text.
     */
    public void stopScan() {
        btn_Scan.setText("Scan Again");

        mBTLeScanner.stop();
    }

    public void goToSelectedDevicesActivity() {
        ArrayList<BTLE_Device> selectedDevicesArrayList = new ArrayList<>(selectedDevices);
        Intent intent = new Intent(this, SelectedDevicesActivity.class);
        intent.putParcelableArrayListExtra("selectedDevices", selectedDevicesArrayList);
        startActivity(intent);
    }

    private void updateGreeting() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            greetingTextView.setText("Hello, " + userEmail);
        }
    }

    private FirebaseAuth.AuthStateListener authStateListener = new FirebaseAuth.AuthStateListener() {
        @Override
        public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
            updateGreeting();
        }
    };

}
