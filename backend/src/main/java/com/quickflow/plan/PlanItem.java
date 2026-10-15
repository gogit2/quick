package com.quickflow.plan;

import com.quickflow.common.PlanItemSourceType;

/** One item of a plan. The title is the current title of the source (contract schema PlanItem). */
public record PlanItem(
        Long id,
        PlanItemSourceType sourceType,
        Long sourceId,
        String title,
        boolean done) {
}
