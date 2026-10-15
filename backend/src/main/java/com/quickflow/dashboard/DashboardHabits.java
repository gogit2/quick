package com.quickflow.dashboard;

import com.quickflow.habit.Habit;
import java.util.List;

/** Habit figures of the dashboard (contract schema DashboardHabits). items = active habits (today's checklist). */
public record DashboardHabits(
        int active,
        int completedToday,
        List<Habit> items) {
}
