package com.example.campuscompanion;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;

public class TaskDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TASK = "extra_task";

    private static final String TAG            = "TaskDetailActivity";
    private static final String KEY_COMPLETED  = "key_completed";

    private TextView     tvDetailTitle;
    private TextView     tvDetailDescription;
    private TextView     tvDetailPriority;
    private MaterialButton btnMarkComplete;

    private Task    task;
    private boolean isCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_task_detail);

        tvDetailTitle       = findViewById(R.id.tvDetailTitle);
        tvDetailDescription = findViewById(R.id.tvDetailDescription);
        tvDetailPriority    = findViewById(R.id.tvDetailPriority);
        btnMarkComplete     = findViewById(R.id.btnMarkComplete);

        // Defensive: handle missing or wrong-type extra
        Object extra = getIntent().getSerializableExtra(EXTRA_TASK);
        if (extra instanceof Task) {
            task = (Task) extra;
        } else {
            Log.w(TAG, "EXTRA_TASK missing or wrong type — finishing activity");
            Toast.makeText(this, getString(R.string.error_task_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Restore completed state after rotation
        if (savedInstanceState != null) {
            isCompleted = savedInstanceState.getBoolean(KEY_COMPLETED, false);
        }

        populateUI();

        btnMarkComplete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isCompleted) {
                    showConfirmationDialog();
                }
            }
        });
    }

    private void populateUI() {
        tvDetailTitle.setText(task.getTitle());
        tvDetailDescription.setText(task.getDescription());
        tvDetailPriority.setText(getString(R.string.priority_label, task.getPriority()));

        int colorRes;
        switch (task.getPriority()) {
            case Task.PRIORITY_HIGH:
                colorRes = R.color.priority_high;
                break;
            case Task.PRIORITY_MEDIUM:
                colorRes = R.color.priority_medium;
                break;
            default:
                colorRes = R.color.priority_low;
                break;
        }
        tvDetailPriority.setTextColor(ContextCompat.getColor(this, colorRes));

        updateCompletionUI();
    }

    private void updateCompletionUI() {
        if (isCompleted) {
            btnMarkComplete.setText(getString(R.string.btn_completed));
            btnMarkComplete.setEnabled(false);
            btnMarkComplete.setAlpha(0.6f);
        } else {
            btnMarkComplete.setText(getString(R.string.btn_mark_complete));
            btnMarkComplete.setEnabled(true);
            btnMarkComplete.setAlpha(1.0f);
        }
    }

    private void showConfirmationDialog() {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_confirm_title))
                .setMessage(getString(R.string.dialog_confirm_message, task.getTitle()))
                .setPositiveButton(getString(R.string.dialog_yes), (dialog, which) -> {
                    isCompleted = true;
                    task.setCompleted(true);
                    updateCompletionUI();
                    Toast.makeText(this,
                            getString(R.string.toast_task_complete, task.getTitle()),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.dialog_cancel), null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_COMPLETED, isCompleted);
        Log.d(TAG, "onSaveInstanceState called, isCompleted=" + isCompleted);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d(TAG, "onStart called");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume called");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy called");
    }
}