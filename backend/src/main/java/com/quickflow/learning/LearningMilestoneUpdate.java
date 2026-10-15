package com.quickflow.learning;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request body of PUT /api/learning-cards/{id}/milestones/{milestoneId} (contract schema LearningMilestoneUpdate). */
public record LearningMilestoneUpdate(
        @NotBlank @Size(max = 200) String title,
        @NotNull Boolean done,
        LocalDate targetDate) {
}
