package com.quickflow.plan;

/** Filter of GET /api/plans. ACTIVE_UPCOMING = NOT_STARTED + IN_PROGRESS. Contract schema PlanGroup. */
public enum PlanGroup {
    ACTIVE_UPCOMING,
    COMPLETED
}
