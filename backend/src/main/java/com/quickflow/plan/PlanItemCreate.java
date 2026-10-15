package com.quickflow.plan;

import com.quickflow.common.PlanItemSourceType;
import jakarta.validation.constraints.NotNull;

/** One source of a new plan (contract schema PlanItemCreate). Existence is checked by PlanService. */
public record PlanItemCreate(
        @NotNull PlanItemSourceType sourceType,
        @NotNull Long sourceId) {
}
