package com.quickflow.learning;

import java.time.Instant;
import java.time.LocalDate;

/** Response body of a milestone (contract schema LearningMilestone). */
public record LearningMilestone(
        Long id,
        Long learningCardId,
        String title,
        boolean done,
        LocalDate targetDate,
        Instant completedAt) {
}
