package com.quickflow.plan;

import java.time.Duration;
import java.time.Instant;

/** Plan status, rest time and progress (D-2, BR12). Plain logic: no clock, no repository. */
public final class PlanStatusCalculator {

    private PlanStatusCalculator() {
    }

    /**
     * COMPLETED if there are items and all are done, or if now is at or after the end (D-2).
     * Otherwise IN_PROGRESS from the start (inclusive) and NOT_STARTED before it.
     */
    public static PlanStatus statusOf(Instant start, Instant end, int doneItems, int totalItems, Instant now) {
        if (totalItems > 0 && doneItems == totalItems) {
            return PlanStatus.COMPLETED;
        }
        if (!now.isBefore(end)) {
            return PlanStatus.COMPLETED;
        }
        if (!now.isBefore(start)) {
            return PlanStatus.IN_PROGRESS;
        }
        return PlanStatus.NOT_STARTED;
    }

    /** Seconds until the end (never negative), only while IN_PROGRESS (BR12). Null otherwise. */
    public static Long restSeconds(PlanStatus status, Instant end, Instant now) {
        if (status != PlanStatus.IN_PROGRESS) {
            return null;
        }
        return Math.max(0L, Duration.between(now, end).getSeconds());
    }

    /** Round-half-up of done*100/total, 0 when there are no items. */
    public static int completionPercent(int doneItems, int totalItems) {
        if (totalItems == 0) {
            return 0;
        }
        return (int) ((2L * doneItems * 100 + totalItems) / (2L * totalItems));
    }
}
