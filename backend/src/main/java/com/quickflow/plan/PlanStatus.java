package com.quickflow.plan;

/** Computed on every read from the clock and the item flags (never stored). Contract schema PlanStatus. */
public enum PlanStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED
}
