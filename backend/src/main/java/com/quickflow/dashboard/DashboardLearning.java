package com.quickflow.dashboard;

import com.quickflow.learning.LearningMilestone;
import java.util.List;

/** Learning figures of the dashboard (contract schema DashboardLearning). recentMilestones = last 7 days, newest first. */
public record DashboardLearning(
        int totalCards,
        int notStarted,
        int inProgress,
        int completed,
        int milestonesDone,
        int milestonesTotal,
        List<LearningMilestone> recentMilestones) {
}
