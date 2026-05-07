package com.example.locationlogger;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * LocationService – a started foreground service that:
 *  1. Acquires periodic location updates via FusedLocationProviderClient.
 *  2. Appends each update to an internal file (location_log.txt).
 *  3. Broadcasts each update to MainActivity via LocalBroadcastManager.
 *
 * Lifecycle:
 *  • Started with ACTION_START when the user flips the switch ON.
 *  • Stopped with ACTION_STOP when the user flips the switch OFF.
 *  • The service stops itself (stopSelf) on any unrecoverable error.
 */
public class LocationService extends Service {

    private static final String TAG = "LocationService";

    // ── Notification constants ────────────────────────────────────────────────
    private static final String CHANNEL_ID      = "location_logger_channel";
    private static final String CHANNEL_NAME    = "Location Tracking";
    private static final int    NOTIFICATION_ID = 101;

    // ── Public contract ───────────────────────────────────────────────────────
    public static final String ACTION_START   = "ACTION_START";
    public static final String ACTION_STOP    = "ACTION_STOP";
    public static final String LOG_FILE_NAME  = "location_log.txt";

    // ── Location update settings ──────────────────────────────────────────────
    /** Desired interval between updates (ms). */
    private static final long UPDATE_INTERVAL_MS      = 10_000L; // 10 s
    /** Fastest the client will accept an update (ms). */
    private static final long FASTEST_UPDATE_INTERVAL = 5_000L;  //  5 s

    // ── Internal state ────────────────────────────────────────────────────────
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback            locationCallback;
    private boolean                     isTracking = false;

    // ── Service lifecycle ─────────────────────────────────────────────────────

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        buildLocationCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            // System restarted the service with a null intent – stop cleanly.
            Log.w(TAG, "onStartCommand received null intent, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        String action = intent.getAction();
        if (action == null) {
            Log.w(TAG, "onStartCommand received intent with no action, stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }

        switch (action) {
            case ACTION_START:
                promoteToForeground();
                startLocationUpdates();
                break;

            case ACTION_STOP:
                stopLocationUpdates();
                // Remove the persistent notification and stop the foreground state
                stopForeground(true);
                stopSelf();
                break;

            default:
                Log.w(TAG, "Unknown action received: " + action);
                break;
        }

        // START_NOT_STICKY: if the OS kills the service, do NOT restart it automatically.
        // The user must explicitly toggle tracking back on.
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates(); // safety net in case service is killed externally
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        // This is a started service, not a bound service.
        return null;
    }

    // ── Foreground notification ───────────────────────────────────────────────

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW  // low = no sound/vibration
            );
            channel.setDescription("Active while location tracking is running");

            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            } else {
                Log.e(TAG, "NotificationManager unavailable – channel not created.");
            }
        }
    }

    private void promoteToForeground() {
        try {
            // Tapping the notification re-opens MainActivity
            Intent openApp = new Intent(this, MainActivity.class);
            openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            int piFlags = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    ? PendingIntent.FLAG_IMMUTABLE
                    : 0;
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 0, openApp, piFlags);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Location Logger")
                    .setContentText("Tracking your location…")
                    .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)          // cannot be dismissed by the user
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .build();

            // On API 34+ we must specify the foreground service type.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to promote service to foreground: " + e.getMessage());
            broadcastError("Could not start foreground service.");
            stopSelf();
        }
    }

    // ── Location updates ──────────────────────────────────────────────────────

    private void buildLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult result) {
                if (result == null) {
                    Log.w(TAG, "LocationResult was null – skipping update.");
                    return;
                }
                for (Location location : result.getLocations()) {
                    if (location != null) {
                        handleNewLocation(location);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        // Guard: make sure we have at least coarse location permission.
        boolean fineOk = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean coarseOk = ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;

        if (!fineOk && !coarseOk) {
            Log.e(TAG, "Location permissions not granted.");
            broadcastError("Location permission not granted.");
            stopForeground(true);
            stopSelf();
            return;
        }

        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

            // Build a location request. The new LocationRequest.Builder API is
            // available in play-services-location 21+.
            LocationRequest locationRequest = new LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MS)
                    .setMinUpdateIntervalMillis(FASTEST_UPDATE_INTERVAL)
                    .build();

            // Request updates on the main looper so the callback runs on the UI thread.
            // Because we use LocalBroadcastManager the broadcast is still thread-safe.
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

            isTracking = true;
            Log.i(TAG, "Location updates started.");

        } catch (SecurityException se) {
            // Permission revoked between check and request – handle gracefully.
            Log.e(TAG, "SecurityException starting location updates: " + se.getMessage());
            broadcastError("Location permission was revoked.");
            stopForeground(true);
            stopSelf();

        } catch (Exception e) {
            Log.e(TAG, "Unexpected error starting location updates: " + e.getMessage());
            broadcastError("Failed to start location updates.");
            stopForeground(true);
            stopSelf();
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && isTracking) {
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback);
                Log.i(TAG, "Location updates stopped.");
            } catch (Exception e) {
                Log.e(TAG, "Error stopping location updates: " + e.getMessage());
            } finally {
                isTracking = false;
            }
        }
    }

    // ── Data handling ─────────────────────────────────────────────────────────

    private void handleNewLocation(Location location) {
        double lat  = location.getLatitude();
        double lon  = location.getLongitude();
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date(location.getTime()));

        String entry = time
                + " | Lat: " + String.format(Locale.getDefault(), "%.6f", lat)
                + " | Lon: " + String.format(Locale.getDefault(), "%.6f", lon);

        appendToLogFile(entry);
        broadcastLocationUpdate(lat, lon, time);
    }

    /**
     * Appends a single entry to the internal log file.
     * MODE_APPEND ensures we never overwrite earlier entries.
     */
    private void appendToLogFile(String entry) {
        try (FileOutputStream fos = openFileOutput(LOG_FILE_NAME, MODE_APPEND)) {
            fos.write((entry + "\n").getBytes());
            Log.d(TAG, "Logged: " + entry);
        } catch (IOException e) {
            Log.e(TAG, "IOException writing to log file: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error writing to log file: " + e.getMessage());
        }
    }

    // ── Broadcast helpers ─────────────────────────────────────────────────────

    private void broadcastLocationUpdate(double lat, double lon, String timestamp) {
        Intent intent = new Intent(MainActivity.ACTION_LOCATION_UPDATE);
        intent.putExtra(MainActivity.EXTRA_LATITUDE,  lat);
        intent.putExtra(MainActivity.EXTRA_LONGITUDE, lon);
        intent.putExtra(MainActivity.EXTRA_TIMESTAMP, timestamp);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastError(String message) {
        Intent intent = new Intent(MainActivity.ACTION_LOCATION_UPDATE);
        intent.putExtra(MainActivity.EXTRA_ERROR, message);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
}
