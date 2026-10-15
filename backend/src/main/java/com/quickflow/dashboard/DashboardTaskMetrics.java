package com.quickflow.dashboard;

/** Task figures of the dashboard (contract schema DashboardTaskMetrics). Archived tasks are not counted. */
public record DashboardTaskMetrics(
        int dueToday,
        int overdue,
        int completedToday,
        int total,
        int completed,
        int completionPercent) {
}
