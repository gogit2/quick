package com.quickflow.dashboard;

import com.quickflow.learning.LearningCard;
import com.quickflow.learning.LearningMilestone;
import com.quickflow.task.Task;
import com.quickflow.task.TaskStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

/**
 * Dashboard rules that are not already owned by another service. Plain logic: no clock, no repository;
 * the dates and the zone are passed in.
 */
public final class DashboardRules {

    /** "Last 7 days" = the day itself and the 6 calendar days before it, in the user time zone (D-4). */
    static final int RECENT_DAYS = 7;

    private DashboardRules() {
    }

    /** completedToday (D-15): a DONE task whose last update falls on the day, in the user time zone. */
    public static boolean completedOn(Task task, LocalDate day, ZoneId zone) {
        return task.status() == TaskStatus.DONE
                && LocalDate.ofInstant(task.updatedAt(), zone).equals(day);
    }

    /** recentMilestones (US-DSH-06): milestones completed in the last 7 days, newest first. */
    public static List<LearningMilestone> recentMilestones(List<LearningCard> cards, LocalDate today, ZoneId zone) {
        LocalDate earliest = today.minusDays(RECENT_DAYS - 1);
        return cards.stream()
                .flatMap(card -> card.milestones().stream())
                .filter(m -> m.done() && m.completedAt() != null)
                .filter(m -> !LocalDate.ofInstant(m.completedAt(), zone).isBefore(earliest))
                .sorted(Comparator.comparing(LearningMilestone::completedAt, Comparator.reverseOrder())
                        .thenComparing(LearningMilestone::id, Comparator.reverseOrder()))
                .toList();
    }
}
