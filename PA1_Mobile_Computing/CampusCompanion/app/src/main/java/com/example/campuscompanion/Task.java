package com.example.campuscompanion;

import java.io.Serializable;

public class Task implements Serializable {

    public static final String PRIORITY_HIGH   = "High";
    public static final String PRIORITY_MEDIUM = "Medium";
    public static final String PRIORITY_LOW    = "Low";

    private final int    id;
    private final String title;
    private final String description;
    private final String priority;
    private       boolean completed;

    public Task(int id, String title, String description, String priority) {
        this.id          = id;
        this.title       = title;
        this.description = description;
        this.priority    = priority;
        this.completed   = false;
    }

    public int    getId()          { return id; }
    public String getTitle()       { return title; }
    public String getDescription() { return description; }
    public String getPriority()    { return priority; }
    public boolean isCompleted()   { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
