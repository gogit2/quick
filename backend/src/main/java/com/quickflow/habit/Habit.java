package com.quickflow.habit;

import java.time.Instant;
import java.time.LocalDate;

/** Response body of the habit operations (contract schema Habit). Derived fields are computed for today. */
public record Habit(
        Long id,
        String name,
        String description,
        HabitFrequency frequency,
        Instant createdAt,
        boolean active,
        boolean doneForCurrentPeriod,
        int currentStreak,
        int totalCompletions,
        LocalDate periodStart,
        LocalDate periodEnd) {
}
