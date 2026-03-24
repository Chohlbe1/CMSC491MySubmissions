package com.example.campuscompanion;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.campuscompanion.R;
import com.example.campuscompanion.Task;
import com.example.campuscompanion.TaskAdapter;
import com.example.campuscompanion.TaskDetailActivity;
import com.example.campuscompanion.TaskRepository;

import java.util.List;

public class TaskListActivity extends AppCompatActivity {

    public static final String EXTRA_USER_NAME = "extra_user_name";

    private static final String TAG = "TaskListActivity";

    private TextView   tvWelcome;
    private ListView   lvTasks;
    private List<Task> taskList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");
        setContentView(R.layout.activity_task_list);
        tvWelcome = findViewById(R.id.tvWelcome);
        lvTasks   = findViewById(R.id.lvTasks);

        // Retrieve user name passed via Intent
        String userName = getIntent().getStringExtra(EXTRA_USER_NAME);
        if (userName == null || userName.isEmpty()) {
            userName = getString(R.string.default_user);
        }
        tvWelcome.setText(getString(R.string.welcome_message, userName));

        taskList = TaskRepository.getAllTasks();

        TaskAdapter adapter = new TaskAdapter(this, taskList);
        lvTasks.setAdapter(adapter);

        lvTasks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Task selected = taskList.get(position);
                Intent intent = new Intent(TaskListActivity.this, TaskDetailActivity.class);
                intent.putExtra(TaskDetailActivity.EXTRA_TASK, selected);
                startActivity(intent);
            }
        });
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