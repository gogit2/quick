package com.quickflow.dashboard;

import com.quickflow.plan.Plan;
import java.util.List;

/** Plan figures of the dashboard (contract schema DashboardPlans). items = IN_PROGRESS plans by priorityOrder. */
public record DashboardPlans(
        int active,
        int upcoming,
        int completed,
        List<Plan> items) {
}
