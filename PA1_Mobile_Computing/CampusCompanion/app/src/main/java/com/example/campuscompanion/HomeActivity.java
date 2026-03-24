package com.example.campuscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final String KEY_USER_NAME = "key_user_name";

    private TextInputLayout tilUserName;
    private TextInputEditText etUserName;
    private MaterialButton btnGoToTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_home);

        tilUserName = findViewById(R.id.tilUserName);
        etUserName = findViewById(R.id.etUserName);
        btnGoToTasks = findViewById(R.id.btnGoToTasks);

        // Restore saved state after rotation
        if (savedInstanceState != null) {
            String savedName = savedInstanceState.getString(KEY_USER_NAME, "");
            etUserName.setText(savedName);
        }

        btnGoToTasks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToTaskList();
            }
        });
    }

    private void navigateToTaskList() {
        String userName = etUserName.getText() != null
                ? etUserName.getText().toString().trim()
                : "";

        if (TextUtils.isEmpty(userName)) {
            tilUserName.setError(getString(R.string.error_name_empty));
            return;
        }

        tilUserName.setError(null);
        Intent intent = new Intent(HomeActivity.this, TaskListActivity.class);
        intent.putExtra(TaskListActivity.EXTRA_USER_NAME, userName);
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(TAG, "onSaveInstanceState called");
        if (etUserName.getText() != null) {
            outState.putString(KEY_USER_NAME, etUserName.getText().toString());
        }
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
