package com.quickflow.learning;

import java.time.Instant;
import java.util.List;

/** Response body of the learning card operations (contract schema LearningCard). */
public record LearningCard(
        Long id,
        String title,
        String description,
        LearningStatus status,
        Instant createdAt,
        List<LearningMilestone> milestones,
        List<LearningNote> notes) {
}
