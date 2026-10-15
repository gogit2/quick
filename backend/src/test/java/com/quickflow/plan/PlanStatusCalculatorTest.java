package com.quickflow.plan;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlanStatusCalculatorTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T11:00:00Z");

    @Test
    void justBeforeStartIsNotStartedWithoutRest() {
        Instant now = START.minusSeconds(1);
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 2, now)).isEqualTo(PlanStatus.NOT_STARTED);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.NOT_STARTED, END, now)).isNull();
    }

    @Test
    void exactlyAtStartIsInProgress() {
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 2, START)).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.IN_PROGRESS, END, START)).isEqualTo(7200L);
    }

    @Test
    void insideWindowIsInProgressWithRestTime() {
        Instant now = Instant.parse("2026-10-15T10:30:00Z");
        assertThat(PlanStatusCalculator.statusOf(START, END, 1, 2, now)).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.IN_PROGRESS, END, now)).isEqualTo(1800L);
    }

    @Test
    void exactlyAtEndIsCompletedWithoutRest() {
        assertThat(PlanStatusCalculator.statusOf(START, END, 1, 2, END)).isEqualTo(PlanStatus.COMPLETED);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.COMPLETED, END, END)).isNull();
    }

    @Test
    void afterEndWithOpenItemsIsCompleted() {
        Instant now = END.plusSeconds(3600);
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 2, now)).isEqualTo(PlanStatus.COMPLETED);
    }

    @Test
    void allItemsDoneBeforeStartIsCompleted() {
        Instant now = START.minusSeconds(60);
        assertThat(PlanStatusCalculator.statusOf(START, END, 2, 2, now)).isEqualTo(PlanStatus.COMPLETED);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.COMPLETED, END, now)).isNull();
    }

    @Test
    void allItemsDoneInsideWindowIsCompletedWithoutRest() {
        Instant now = Instant.parse("2026-10-15T10:00:00Z");
        assertThat(PlanStatusCalculator.statusOf(START, END, 2, 2, now)).isEqualTo(PlanStatus.COMPLETED);
        assertThat(PlanStatusCalculator.restSeconds(PlanStatus.COMPLETED, END, now)).isNull();
    }

    @Test
    void planWithoutItemsFollowsTheClockOnly() {
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 0, START.minusSeconds(1)))
                .isEqualTo(PlanStatus.NOT_STARTED);
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 0, START)).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(PlanStatusCalculator.statusOf(START, END, 0, 0, END)).isEqualTo(PlanStatus.COMPLETED);
    }

    @Test
    void percentRoundsHalfUp() {
        assertThat(PlanStatusCalculator.completionPercent(1, 3)).isEqualTo(33);
        assertThat(PlanStatusCalculator.completionPercent(2, 3)).isEqualTo(67);
        assertThat(PlanStatusCalculator.completionPercent(1, 8)).isEqualTo(13);
        assertThat(PlanStatusCalculator.completionPercent(3, 3)).isEqualTo(100);
    }

    @Test
    void zeroItemsGivesZeroPercent() {
        assertThat(PlanStatusCalculator.completionPercent(0, 0)).isZero();
    }
}
