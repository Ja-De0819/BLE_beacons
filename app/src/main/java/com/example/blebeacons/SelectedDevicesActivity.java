package com.example.blebeacons;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class SelectedDevicesActivity extends AppCompatActivity {

    private ListView listView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_selected_devices);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();

        listView = findViewById(R.id.listViewSelectedDevices);

        // Retrieve beacon data from Firestore and populate the list
        populateBeaconListFromFirestore();

        Button btnStartMonitoring = findViewById(R.id.btnStartMonitoring);
        btnStartMonitoring.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start the MonitoringActivity
                Intent intent = new Intent(SelectedDevicesActivity.this, MonitoringActivity.class);
                startActivity(intent);
            }
        });
    }

    private void populateBeaconListFromFirestore() {
        CollectionReference userBeaconsCollection = db.collection("beacons").document(currentUserId).collection("userBeacons");

        // Fetch beacon data from Firestore
        userBeaconsCollection.get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
            @Override
            public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                List<String> beaconInfoList = new ArrayList<>();
                for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                    // Get beacon data and add to list
                    String beaconName = document.getString("name");
                    String beaconAddress = document.getString("address");
                    int beaconRSSI = document.getLong("rssi").intValue();
                    String deviceInfo = "Name: " + beaconName + "\nAddress: " + beaconAddress + "\nRSSI: " + beaconRSSI;
                    beaconInfoList.add(deviceInfo);
                }

                // Create and set adapter for ListView
                ArrayAdapter<String> adapter = new ArrayAdapter<>(SelectedDevicesActivity.this, android.R.layout.simple_list_item_1, beaconInfoList);
                listView.setAdapter(adapter);

                // Handle item click to prompt for custom name
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        DocumentSnapshot selectedDocument = queryDocumentSnapshots.getDocuments().get(position);
                        promptForCustomName(selectedDocument);
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                // Handle failure
            }
        });
    }

    private void promptForCustomName(final DocumentSnapshot documentSnapshot) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter Custom Name");

        // Set up the input
        final EditText input = new EditText(this);
        input.setText(documentSnapshot.getString("name")); // Set default text to current name
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String customName = input.getText().toString();
                updateBeaconNameInFirestore(documentSnapshot, customName);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateBeaconNameInFirestore(DocumentSnapshot documentSnapshot, String customName) {
        documentSnapshot.getReference().update("name", customName)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Update successful
                        populateBeaconListFromFirestore(); // Refresh the list after updating
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle failure
                    }
                });
    }
}
