package com.quickflow.habit;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

/** Current streak of a habit (D-6). Plain logic, unit-testable: no clock, no repository. */
public final class StreakCalculator {

    private StreakCalculator() {
    }

    /**
     * Consecutive completed periods (days or weeks) ending at the current period. A current period that is
     * not done yet does not break the streak: the count then ends at the previous period.
     */
    public static int currentStreak(HabitFrequency frequency, LocalDate today, Set<LocalDate> completionDates) {
        Set<LocalDate> donePeriods = completionDates.stream()
                .map(date -> HabitPeriods.periodStart(date, frequency))
                .collect(Collectors.toSet());
        LocalDate period = HabitPeriods.periodStart(today, frequency);
        if (!donePeriods.contains(period)) {
            period = HabitPeriods.previous(period, frequency);
        }
        int streak = 0;
        while (donePeriods.contains(period)) {
            streak++;
            period = HabitPeriods.previous(period, frequency);
        }
        return streak;
    }
}
