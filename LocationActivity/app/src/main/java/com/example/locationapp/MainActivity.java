package com.example.locationapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    // Step 01: UI field references
    private TextView tvLatitude;
    private TextView tvLongitude;
    private Button btnGetLocation;

    // Step 04: Location client field
    private FusedLocationProviderClient fusedLocationProviderClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Step 01: Bind UI fields
        tvLatitude     = findViewById(R.id.tvLatitude);
        tvLongitude    = findViewById(R.id.tvLongitude);
        btnGetLocation = findViewById(R.id.btnGetLocation);

        // Step 04: Initialize FusedLocationProviderClient
        fusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(this);

        // Button triggers permission check → location fetch
        btnGetLocation.setOnClickListener(v -> requestLocationIfPermitted());
    }

    // Step 03: Check / request runtime permission
    private void requestLocationIfPermitted() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        }
    }

    // Step 05: Retrieve last known location asynchronously
    private void fetchLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        // Step 06: Display values on screen
                        tvLatitude.setText(String.valueOf(location.getLatitude()));
                        tvLongitude.setText(String.valueOf(location.getLongitude()));
                    } else {
                        tvLatitude.setText(getString(R.string.unavailable));
                        tvLongitude.setText(getString(R.string.unavailable));
                        Toast.makeText(this,
                                getString(R.string.location_unavailable_msg),
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, e ->
                        Toast.makeText(this,
                                getString(R.string.location_error_msg),
                                Toast.LENGTH_LONG).show()
                );
    }

    // Step 07: Handle denied permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                // Permission denied – show Toast explaining requirement
                Toast.makeText(this,
                        getString(R.string.permission_denied_msg),
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}
