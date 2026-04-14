package com.example.phoneorientation;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    // ── Sensor infrastructure ────────────────────────────────────────────────
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor gyroscope;

    private static final float ALPHA      = 0.96f;
    private static final float NS_TO_S    = 1.0f / 1_000_000_000.0f;
    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);

    // ── Sensor state ─────────────────────────────────────────────────────────
    private final float[] accelValues = new float[3];
    private final float[] gyroValues  = new float[3];
    private boolean accelReady = false;

    // Orientation angles (degrees)
    private float pitch = 0f;
    private float roll  = 0f;
    private float yaw   = 0f;

    private long lastGyroTimestampNs = 0L;

    // ── App state ────────────────────────────────────────────────────────────
    /** True when the toggle switch is ON and readings should be live. */
    private boolean isActive = false;

    // ── UI references ────────────────────────────────────────────────────────
    private SwitchMaterial sensorSwitch;
    private MaterialButton btnResetYaw;
    private View            bannerOff;

    // Accelerometer TextViews
    private TextView tvAccelX, tvAccelY, tvAccelZ, tvAccelStatus;

    // Gyroscope TextViews
    private TextView tvGyroX, tvGyroY, tvGyroZ, tvGyroStatus;

    // Orientation TextViews
    private TextView tvPitch, tvRoll, tvYaw;

    // Custom arrow indicator
    private ArrowView arrowView;

    // ── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bindViews();
        initSensors();
        setupSwitch();
        setupResetButton();

        // App starts with sensors OFF
        showIdleState();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Only re-register if the switch was left ON before the activity paused
        if (isActive) {
            registerSensors();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Always unregister when the activity leaves the foreground
        unregisterSensors();
    }

    // ── View binding ─────────────────────────────────────────────────────────

    private void bindViews() {
        sensorSwitch = findViewById(R.id.sensorSwitch);
        btnResetYaw  = findViewById(R.id.btnResetYaw);
        bannerOff    = findViewById(R.id.bannerOff);

        tvAccelX      = findViewById(R.id.tvAccelX);
        tvAccelY      = findViewById(R.id.tvAccelY);
        tvAccelZ      = findViewById(R.id.tvAccelZ);
        tvAccelStatus = findViewById(R.id.tvAccelStatus);

        tvGyroX      = findViewById(R.id.tvGyroX);
        tvGyroY      = findViewById(R.id.tvGyroY);
        tvGyroZ      = findViewById(R.id.tvGyroZ);
        tvGyroStatus = findViewById(R.id.tvGyroStatus);

        tvPitch  = findViewById(R.id.tvPitch);
        tvRoll   = findViewById(R.id.tvRoll);
        tvYaw    = findViewById(R.id.tvYaw);

        arrowView = findViewById(R.id.arrowView);
    }

    // ── Sensor initialisation ─────────────────────────────────────────────────

    private void initSensors() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope     = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Show availability status even before the switch is toggled
        tvAccelStatus.setText(accelerometer != null
                ? getString(R.string.sensor_available)
                : getString(R.string.sensor_unavailable));

        tvGyroStatus.setText(gyroscope != null
                ? getString(R.string.sensor_available)
                : getString(R.string.sensor_unavailable));
    }

    // ── Switch ────────────────────────────────────────────────────────────────

    private void setupSwitch() {
        // Default state is unchecked (OFF) – set in XML, confirmed here
        sensorSwitch.setChecked(false);

        sensorSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isActive = isChecked;
            if (isActive) {
                resetOrientation();
                registerSensors();
                bannerOff.setVisibility(View.GONE);
                btnResetYaw.setEnabled(true);
            } else {
                unregisterSensors();
                showIdleState();
                bannerOff.setVisibility(View.VISIBLE);
                btnResetYaw.setEnabled(false);
            }
        });
    }

    // ── Reset Yaw button ──────────────────────────────────────────────────────

    private void setupResetButton() {
        btnResetYaw.setEnabled(false); // disabled until sensors are on

        btnResetYaw.setOnClickListener(v -> {
            // Zero out yaw while preserving the current pitch/roll estimate
            yaw = 0f;
            // Restart gyro integration from this moment
            lastGyroTimestampNs = 0L;
            tvYaw.setText(formatAngle(yaw));
        });
    }

    // ── Sensor registration ───────────────────────────────────────────────────

    private void registerSensors() {
        if (accelerometer != null) {
            sensorManager.registerListener(
                    this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        if (gyroscope != null) {
            sensorManager.registerListener(
                    this, gyroscope, SensorManager.SENSOR_DELAY_UI);
        }
    }

    private void unregisterSensors() {
        sensorManager.unregisterListener(this);
    }

    // ── SensorEventListener ───────────────────────────────────────────────────

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (!isActive) return;

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                handleAccelerometer(event);
                break;
            case Sensor.TYPE_GYROSCOPE:
                handleGyroscope(event);
                break;
            default:
                break;
        }

        updateOrientationDisplay();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Required by the interface; not used here
    }

    // ── Sensor processing ─────────────────────────────────────────────────────

    /**
     * Stores the latest accelerometer readings and updates the raw data display.
     * When no gyroscope data has arrived yet, the accelerometer is used alone
     * to provide an initial static estimate of pitch and roll.
     */
    private void handleAccelerometer(SensorEvent event) {
        accelValues[0] = event.values[0];
        accelValues[1] = event.values[1];
        accelValues[2] = event.values[2];
        accelReady = true;

        tvAccelX.setText(formatSensorValue(accelValues[0]));
        tvAccelY.setText(formatSensorValue(accelValues[1]));
        tvAccelZ.setText(formatSensorValue(accelValues[2]));

        // Seed orientation from accelerometer if gyroscope hasn't fired yet
        if (lastGyroTimestampNs == 0L) {
            pitch = computeAccelPitch();
            roll  = computeAccelRoll();
        }
    }

    /**
     * Applies a complementary filter each time the gyroscope fires:
     *
     *   angle = α × (angle + ω × Δt) + (1−α) × accel_angle
     *
     * This blends the gyroscope's precise short-term motion with the
     * accelerometer's drift-free long-term reference for pitch and roll.
     * Yaw is integrated from the gyroscope only (no absolute reference).
     */
    private void handleGyroscope(SensorEvent event) {
        gyroValues[0] = event.values[0];   // rad/s around X → pitch change
        gyroValues[1] = event.values[1];   // rad/s around Y → roll change
        gyroValues[2] = event.values[2];   // rad/s around Z → yaw change

        tvGyroX.setText(formatSensorValue(gyroValues[0]));
        tvGyroY.setText(formatSensorValue(gyroValues[1]));
        tvGyroZ.setText(formatSensorValue(gyroValues[2]));

        if (lastGyroTimestampNs != 0L && accelReady) {
            float dt = (event.timestamp - lastGyroTimestampNs) * NS_TO_S;

            // Guard against stale or implausibly large time-steps
            if (dt > 0f && dt < 0.5f) {
                float gyroPitchDelta = gyroValues[0] * dt * RAD_TO_DEG;
                float gyroRollDelta  = gyroValues[1] * dt * RAD_TO_DEG;
                float gyroYawDelta   = gyroValues[2] * dt * RAD_TO_DEG;

                // Complementary filter: fuse gyro prediction with accel correction
                pitch = ALPHA * (pitch + gyroPitchDelta) + (1f - ALPHA) * computeAccelPitch();
                roll  = ALPHA * (roll  + gyroRollDelta)  + (1f - ALPHA) * computeAccelRoll();

                // Yaw: integrate gyroscope only; wrap to [−180, 180]
                yaw += gyroYawDelta;
                if (yaw >  180f) yaw -= 360f;
                if (yaw < -180f) yaw += 360f;
            }
        }

        lastGyroTimestampNs = event.timestamp;
    }


    private float computeAccelPitch() {
        return (float) Math.toDegrees(
                Math.atan2(-accelValues[0],
                        Math.sqrt(accelValues[1] * accelValues[1]
                                + accelValues[2] * accelValues[2])));
    }


    private float computeAccelRoll() {
        return (float) Math.toDegrees(
                Math.atan2(accelValues[1], accelValues[2]));
    }


    private void updateOrientationDisplay() {
        tvPitch.setText(formatAngle(pitch));
        tvRoll.setText(formatAngle(roll));
        tvYaw.setText(formatAngle(yaw));

        float arrowAngle = (float) Math.toDegrees(Math.atan2(roll, pitch));
        arrowView.setAngle(arrowAngle);
    }

    /** Resets all accumulated orientation state when the switch is turned on. */
    private void resetOrientation() {
        pitch = 0f;
        roll  = 0f;
        yaw   = 0f;
        accelReady          = false;
        lastGyroTimestampNs = 0L;
    }

    /** Clears all live readings from the display when the switch is turned off. */
    private void showIdleState() {
        String dash = getString(R.string.value_idle);

        tvAccelX.setText(dash);
        tvAccelY.setText(dash);
        tvAccelZ.setText(dash);

        tvGyroX.setText(dash);
        tvGyroY.setText(dash);
        tvGyroZ.setText(dash);

        tvPitch.setText(dash);
        tvRoll.setText(dash);
        tvYaw.setText(dash);

        arrowView.setAngle(0f);
        arrowView.setActive(false);
    }

    // ── Formatting helpers ────────────────────────────────────────────────────

    /** Formats a raw sensor axis value and activates the arrow indicator. */
    private String formatSensorValue(float value) {
        arrowView.setActive(true);
        return String.format(getResources().getConfiguration().locale, "%+.3f", value);
    }

    /** Formats an angle to one decimal place with a degree symbol. */
    private String formatAngle(float degrees) {
        return String.format(getResources().getConfiguration().locale, "%+.1f°", degrees);
    }
}