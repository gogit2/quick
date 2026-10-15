package com.quickflow.plan;

import jakarta.validation.constraints.NotNull;

/** Request body of PUT /api/plans/{id}/items/{itemId} (contract schema PlanItemUpdate). */
public record PlanItemUpdate(
        @NotNull Boolean done) {
}
