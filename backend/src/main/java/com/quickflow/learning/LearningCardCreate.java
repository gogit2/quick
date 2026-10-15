package com.quickflow.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of POST /api/learning-cards (contract schema LearningCardCreate). A null status means NOT_STARTED. */
public record LearningCardCreate(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LearningStatus status) {
}
