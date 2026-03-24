package com.example.campuscompanion;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {

    private static final List<Task> TASKS = new ArrayList<>();

    static { //Randomly made tasks, generalized for most students
        TASKS.add(new Task(1,
                "Submit Lab Report",
                "Complete and submit the Chemistry lab report covering titration experiments conducted in Week 4. Include data tables and error analysis.",
                Task.PRIORITY_HIGH));

        TASKS.add(new Task(2,
                "Study for Midterm Exam",
                "Review chapters 5–9 of the textbook. Focus on recursion, sorting algorithms, and Big-O notation for the Data Structures midterm.",
                Task.PRIORITY_HIGH));

        TASKS.add(new Task(3,
                "Group Project Meeting",
                "Attend the scheduled group meeting to divide responsibilities for the Software Engineering capstone project presentation.",
                Task.PRIORITY_HIGH));

        TASKS.add(new Task(4,
                "Register for Next Semester",
                "Log into the student portal and complete course registration for Fall 2026. Confirm prerequisites are met for upper-level courses.",
                Task.PRIORITY_MEDIUM));

        TASKS.add(new Task(5,
                "Visit Academic Advisor",
                "Schedule and attend a meeting with your academic advisor to review graduation requirements and discuss elective choices.",
                Task.PRIORITY_MEDIUM));

        TASKS.add(new Task(6,
                "Complete Online Readings",
                "Read assigned articles on agile development methodologies posted on the course LMS before Thursday's lecture.",
                Task.PRIORITY_MEDIUM));

        TASKS.add(new Task(7,
                "Pay Tuition Balance",
                "Log into the bursar's office portal and pay the remaining tuition balance to avoid a late fee before the end of the month.",
                Task.PRIORITY_HIGH));

        TASKS.add(new Task(8,
                "Return Library Books",
                "Return two overdue library books: 'Clean Code' and 'The Pragmatic Programmer'. Renew 'Design Patterns' if still needed.",
                Task.PRIORITY_LOW));

        TASKS.add(new Task(9,
                "Update Resume",
                "Add recent internship experience and new technical skills to resume. Tailor for the summer internship application deadline.",
                Task.PRIORITY_MEDIUM));

        TASKS.add(new Task(10,
                "Attend Campus Career Fair",
                "Bring printed resumes and visit booths from tech companies at the Spring Career Fair in the Student Union on Friday.",
                Task.PRIORITY_LOW));
    }

    public static List<Task> getAllTasks() {
        return new ArrayList<>(TASKS);
    }
}
