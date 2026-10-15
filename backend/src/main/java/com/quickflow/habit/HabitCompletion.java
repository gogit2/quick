package com.quickflow.habit;

import java.time.Instant;
import java.time.LocalDate;

/** Response body of a habit completion (contract schema HabitCompletion). */
public record HabitCompletion(
        Long id,
        Long habitId,
        LocalDate completionDate,
        Instant createdAt) {
}
