package com.quickflow.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of POST /api/learning-cards/{id}/notes (contract schema LearningNoteCreate). */
public record LearningNoteCreate(
        @NotBlank @Size(max = 4000) String text) {
}
