package com.quickflow.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request body of POST /api/learning-cards/{id}/milestones (contract schema LearningMilestoneCreate). */
public record LearningMilestoneCreate(
        @NotBlank @Size(max = 200) String title,
        LocalDate targetDate) {
}
