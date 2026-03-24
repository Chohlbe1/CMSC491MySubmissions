package com.example.campuscompanion;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.List;

public class TaskAdapter extends ArrayAdapter<Task> {

    private final Context    context;
    private final List<Task> tasks;

    public TaskAdapter(@NonNull Context context, @NonNull List<Task> tasks) {
        super(context, 0, tasks);
        this.context = context;
        this.tasks   = tasks;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.item_task, parent, false);
            holder = new ViewHolder();
            holder.tvTitle       = convertView.findViewById(R.id.tvTaskTitle);
            holder.tvPriority    = convertView.findViewById(R.id.tvTaskPriority);
            holder.tvDescription = convertView.findViewById(R.id.tvTaskDescription);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Task task = tasks.get(position);
        holder.tvTitle.setText(task.getTitle());
        holder.tvDescription.setText(task.getDescription());
        holder.tvPriority.setText(task.getPriority());

        // Color-code priority badge
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
        holder.tvPriority.setBackgroundTintList(
                ContextCompat.getColorStateList(context, colorRes));

        // Strike-through completed tasks
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() & ~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }

        return convertView;
    }

    static class ViewHolder {
        TextView tvTitle;
        TextView tvPriority;
        TextView tvDescription;
    }
}
