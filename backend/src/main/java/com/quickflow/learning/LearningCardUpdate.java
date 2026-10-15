package com.quickflow.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request body of PUT /api/learning-cards/{id} (contract schema LearningCardUpdate). */
public record LearningCardUpdate(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        LearningStatus status) {
}
