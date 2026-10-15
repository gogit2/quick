package com.quickflow.plan;

import java.time.Instant;
import java.util.List;

/** Response of the plan operations (contract schema Plan). Progress and status are computed per read. */
public record Plan(
        Long id,
        String title,
        int estimatedDurationMinutes,
        Instant startDateTime,
        Instant endDateTime,
        int priorityOrder,
        PlanStatus status,
        Instant createdAt,
        boolean startNotified,
        boolean durationExceedsWindow,
        int totalItems,
        int doneItems,
        int completionPercent,
        Long restSeconds,
        List<PlanItem> items) {
}
