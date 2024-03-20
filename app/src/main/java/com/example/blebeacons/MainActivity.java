package com.example.blebeacons;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {
    private final static String TAG = MainActivity.class.getSimpleName();

    public static final int REQUEST_ENABLE_BT = 1;
    public static final int BTLE_SERVICES = 2;

    private HashMap<String, BTLE_Device> mBTDevicesHashMap;
    private ArrayList<BTLE_Device> mBTDevicesArrayList;
    private ListAdapter_BTLE_Devices adapter;
    private ListView listView;

    private FirebaseAuth mAuth;
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

        mAuth = FirebaseAuth.getInstance();
        greetingTextView = findViewById(R.id.textViewGreeting);
        updateGreeting();

        adapter = new ListAdapter_BTLE_Devices(this, R.layout.btle_device_list_item, mBTDevicesArrayList);

        ListView listView = new ListView(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);

        btn_Scan = findViewById(R.id.btn_scan);
        btn_Logout = findViewById(R.id.btn_logout);

        ((ScrollView) findViewById(R.id.scrollView)).addView(listView);
        findViewById(R.id.btn_scan).setOnClickListener(this);
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

        // Check which request we're responding to
        if (requestCode == REQUEST_ENABLE_BT) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
               Utils.toast(getApplicationContext(), "Thank you for turning on Bluetooth");
            }
            else if (resultCode == RESULT_CANCELED) {
                Utils.toast(getApplicationContext(), "Please turn on Bluetooth");
            }
        }
        else if (requestCode == BTLE_SERVICES) {
            // Do something
        }
    }

    /**
     * Called when an item in the ListView is clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Context context = view.getContext();

        Utils.toast(context, "List Item clicked");

        // do something with the text views and start the next activity.

        stopScan();

        String name = mBTDevicesArrayList.get(position).getName();
        String address = mBTDevicesArrayList.get(position).getAddress();

        Intent intent = new Intent(this, Activity_BTLE_Services.class);
        intent.putExtra(Activity_BTLE_Services.EXTRA_NAME, name);
        intent.putExtra(Activity_BTLE_Services.EXTRA_ADDRESS, address);
        startActivityForResult(intent, BTLE_SERVICES);

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
