package com.example.simplestorageapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    // SharedPreferences key constants
    private static final String PREFS_NAME = "MyAppPrefs";
    private static final String KEY_SAVED_TEXT = "saved_text";

    private EditText editText;
    private Button saveButton;
    private Button loadButton;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Step 1: Wire up the UI
        editText   = findViewById(R.id.editText);
        saveButton = findViewById(R.id.saveButton);
        loadButton = findViewById(R.id.loadButton);

        // Get a handle to SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Step 3: Load any previously saved text on startup (proves persistence)
        loadSavedText();

        // Step 2: Save the text when the Save button is clicked
        saveButton.setOnClickListener(v -> {
            String textToSave = editText.getText().toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(KEY_SAVED_TEXT, textToSave);
            editor.apply();                         // non-blocking write to disk
            Toast.makeText(this, "Saved!", Toast.LENGTH_SHORT).show();
        });

        // Extra load button so the user can reload without restarting
        loadButton.setOnClickListener(v -> loadSavedText());
    }

    /**
     * Reads the stored value from SharedPreferences and populates the EditText.
     * Called both on onCreate() and when the Load button is pressed.
     */
    private void loadSavedText() {
        String savedText = sharedPreferences.getString(KEY_SAVED_TEXT, "");
        editText.setText(savedText);
        if (!savedText.isEmpty()) {
            Toast.makeText(this, "Loaded saved text!", Toast.LENGTH_SHORT).show();
        }
    }
}
