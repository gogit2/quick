package com.quickflow.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickflow.learning.LearningCard;
import com.quickflow.learning.LearningMilestone;
import com.quickflow.learning.LearningStatus;
import com.quickflow.task.Task;
import com.quickflow.task.TaskPriority;
import com.quickflow.task.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Dashboard-only rules: completedToday and recentMilestones, both in the user time zone. */
class DashboardRulesTest {

    /** UTC+4, no daylight saving: 2026-10-15T21:00Z is already 2026-10-16 in this zone. */
    private static final ZoneId DUBAI = ZoneId.of("Asia/Dubai");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Test
    void doneTaskUpdatedOnTheDayInTheUserZoneIsCompletedToday() {
        assertThat(DashboardRules.completedOn(task(TaskStatus.DONE, "2026-10-15T09:00:00Z"), TODAY, DUBAI)).isTrue();
        // 21:00Z is 01:00 on the 16th in Dubai, so it belongs to tomorrow
        assertThat(DashboardRules.completedOn(task(TaskStatus.DONE, "2026-10-15T21:00:00Z"), TODAY, DUBAI)).isFalse();
    }

    @Test
    void notDoneTaskIsNeverCompletedToday() {
        assertThat(DashboardRules.completedOn(task(TaskStatus.TODO, "2026-10-15T09:00:00Z"), TODAY, DUBAI)).isFalse();
        assertThat(DashboardRules.completedOn(task(TaskStatus.IN_PROGRESS, "2026-10-15T09:00:00Z"), TODAY, DUBAI))
                .isFalse();
    }

    @Test
    void recentMilestonesAreDoneMilestonesOfTheLastSevenDaysNewestFirst() {
        LearningCard card = new LearningCard(1L, "Kotlin", null, LearningStatus.IN_PROGRESS, Instant.EPOCH,
                List.of(
                        milestone(1L, true, "2026-10-15T09:00:00Z"),   // today
                        milestone(2L, true, "2026-10-09T09:00:00Z"),   // 6 days back: included (D-4)
                        milestone(3L, true, "2026-10-08T09:00:00Z"),   // 7 days back: excluded
                        milestone(4L, false, "2026-10-14T09:00:00Z"),  // not done: excluded
                        milestone(5L, true, null)),                    // done without completedAt: excluded
                List.of());

        List<LearningMilestone> recent = DashboardRules.recentMilestones(List.of(card), TODAY, DUBAI);

        assertThat(recent).extracting(LearningMilestone::id).containsExactly(1L, 2L);
    }

    @Test
    void recentMilestonesAreOrderedByCompletionAcrossCards() {
        LearningCard older = new LearningCard(1L, "A", null, LearningStatus.IN_PROGRESS, Instant.EPOCH,
                List.of(milestone(10L, true, "2026-10-11T09:00:00Z")), List.of());
        LearningCard newer = new LearningCard(2L, "B", null, LearningStatus.IN_PROGRESS, Instant.EPOCH,
                List.of(milestone(20L, true, "2026-10-14T09:00:00Z")), List.of());

        List<LearningMilestone> recent = DashboardRules.recentMilestones(List.of(older, newer), TODAY, DUBAI);

        assertThat(recent).extracting(LearningMilestone::id).containsExactly(20L, 10L);
    }

    private static Task task(TaskStatus status, String updatedAt) {
        return new Task(1L, "t", null, status, TaskPriority.MEDIUM, null, Instant.EPOCH, Instant.parse(updatedAt),
                false, false);
    }

    private static LearningMilestone milestone(long id, boolean done, String completedAt) {
        return new LearningMilestone(id, 1L, "m" + id, done, null,
                completedAt == null ? null : Instant.parse(completedAt));
    }
}
