package com.quickflow.learning;

import java.time.Instant;

/** Response body of a note (contract schema LearningNote). */
public record LearningNote(
        Long id,
        Long learningCardId,
        String text,
        Instant createdAt) {
}
