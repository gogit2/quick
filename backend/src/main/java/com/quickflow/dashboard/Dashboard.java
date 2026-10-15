package com.quickflow.dashboard;

import com.quickflow.task.Task;
import java.time.LocalDate;
import java.util.List;

/** Response body of GET /api/dashboard (contract schema Dashboard). */
public record Dashboard(
        LocalDate date,
        String timeZone,
        String displayName,
        DashboardTaskMetrics tasks,
        List<Task> todayTasks,
        List<Task> overdueTasks,
        DashboardHabits habits,
        DashboardPlans plans,
        DashboardLearning learning) {
}
