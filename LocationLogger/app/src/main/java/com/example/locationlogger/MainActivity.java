package com.example.locationlogger;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    // ── Broadcast contract (shared with LocationService) ──────────────────────
    public static final String ACTION_LOCATION_UPDATE =
            "com.example.locationlogger.LOCATION_UPDATE";
    public static final String EXTRA_LATITUDE  = "latitude";
    public static final String EXTRA_LONGITUDE = "longitude";
    public static final String EXTRA_TIMESTAMP = "timestamp";
    public static final String EXTRA_ERROR     = "error";

    // ── Views ─────────────────────────────────────────────────────────────────
    private Switch   trackingSwitch;
    private TextView tvStatus;
    private TextView tvLastLocation;
    private TextView tvLog;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean programmaticToggle = false; // prevents re-entrant listener calls

    // ── Broadcast receiver: listens for updates / errors from LocationService ─
    private final BroadcastReceiver locationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            if (intent.hasExtra(EXTRA_ERROR)) {
                // Service reported an error – reflect it in the UI and reset switch
                String error = intent.getStringExtra(EXTRA_ERROR);
                tvStatus.setText("Error: " + error);
                Log.e(TAG, "Service error: " + error);
                setProgrammaticSwitchState(false);
            } else {
                // Successful location update
                double lat       = intent.getDoubleExtra(EXTRA_LATITUDE,  0.0);
                double lon       = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0);
                String timestamp = intent.getStringExtra(EXTRA_TIMESTAMP);

                tvLastLocation.setText(
                        String.format("Lat:  %.6f\nLon:  %.6f\n%s", lat, lon, timestamp));

                // Reload the full log so the list is up-to-date
                loadAndDisplayLog();
            }
        }
    };

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        trackingSwitch  = findViewById(R.id.trackingSwitch);
        tvStatus        = findViewById(R.id.tvStatus);
        tvLastLocation  = findViewById(R.id.tvLastLocation);
        tvLog           = findViewById(R.id.tvLog);

        // Show any previously saved entries on startup
        loadAndDisplayLog();

        trackingSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (programmaticToggle) return; // ignore listener-triggered flips
            if (isChecked) {
                requestPermissionsAndStartTracking();
            } else {
                stopTracking();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register receiver so we only update while the Activity is visible
        LocalBroadcastManager.getInstance(this)
                .registerReceiver(locationReceiver, new IntentFilter(ACTION_LOCATION_UPDATE));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationReceiver);
    }

    // ── Permission handling ───────────────────────────────────────────────────

    private void requestPermissionsAndStartTracking() {
        boolean fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (fineGranted || coarseGranted) {
            startTracking();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;

            if (granted) {
                startTracking();
            } else {
                // Snap the switch back without triggering the listener
                setProgrammaticSwitchState(false);
                tvStatus.setText("Permission Denied – cannot track location");
                Toast.makeText(this,
                        "Location permission is required for tracking.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // ── Service control ───────────────────────────────────────────────────────

    private void startTracking() {
        try {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_START);

            // startForegroundService() required on API 26+ so the service can
            // call startForeground() within 5 seconds of starting.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            tvStatus.setText("Tracking Active");
        } catch (Exception e) {
            Log.e(TAG, "Failed to start LocationService: " + e.getMessage());
            tvStatus.setText("Failed to start service");
            Toast.makeText(this, "Could not start location service.", Toast.LENGTH_SHORT).show();
            setProgrammaticSwitchState(false);
        }
    }

    private void stopTracking() {
        try {
            Intent serviceIntent = new Intent(this, LocationService.class);
            serviceIntent.setAction(LocationService.ACTION_STOP);
            startService(serviceIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to stop LocationService: " + e.getMessage());
        }
        tvStatus.setText("Tracking Stopped");
    }

    // ── Log display ───────────────────────────────────────────────────────────

    /**
     * Reads the internal log file written by LocationService and displays
     * entries in reverse-chronological order (newest first).
     */
    private void loadAndDisplayLog() {
        try {
            FileInputStream fis = openFileInput(LocationService.LOG_FILE_NAME);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));

            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    lines.add(line.trim());
                }
            }
            reader.close();
            fis.close();

            if (lines.isEmpty()) {
                tvLog.setText("No entries recorded yet.");
            } else {
                // Reverse so newest entry is at the top
                Collections.reverse(lines);
                StringBuilder sb = new StringBuilder();
                for (String entry : lines) {
                    sb.append(entry).append("\n");
                }
                tvLog.setText(sb.toString().trim());

                // Also update the "last known" card with the most-recent entry
                // (useful when re-opening the app without active tracking)
                String newest = lines.get(0); // already reversed, so index 0 is newest
                updateLastLocationFromLogLine(newest);
            }

        } catch (FileNotFoundException e) {
            // No log file yet – perfectly normal on first launch
            tvLog.setText("No entries recorded yet.");
        } catch (Exception e) {
            Log.e(TAG, "Error reading log file: " + e.getMessage());
            tvLog.setText("Error reading log.");
        }
    }

    /**
     * Parses a saved log line and fills the "Last Known Location" card.
     * Expected format: "yyyy-MM-dd HH:mm:ss | Lat: 0.000000 | Lon: 0.000000"
     */
    private void updateLastLocationFromLogLine(String line) {
        try {
            // Only update the card from file if the service is NOT actively broadcasting
            // (i.e., tracking is off – avoids overwriting a live update with a stale one)
            if (!trackingSwitch.isChecked()) {
                tvLastLocation.setText(line);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not parse log line for last-location card: " + e.getMessage());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Toggles the switch without firing the OnCheckedChangeListener.
     */
    private void setProgrammaticSwitchState(boolean checked) {
        programmaticToggle = true;
        trackingSwitch.setChecked(checked);
        programmaticToggle = false;
    }
}
