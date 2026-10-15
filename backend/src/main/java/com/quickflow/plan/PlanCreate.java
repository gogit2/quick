package com.quickflow.plan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

/** Request body of POST /api/plans (contract schema PlanCreate). Window and duplicate rules are in PlanService. */
public record PlanCreate(
        @NotBlank @Size(max = 200) String title,
        @NotNull @Min(1) Integer estimatedDurationMinutes,
        @NotNull Instant startDateTime,
        @NotNull Instant endDateTime,
        @NotNull @Min(1) Integer priorityOrder,
        @NotNull @Size(min = 1) @Valid List<PlanItemCreate> items) {
}
