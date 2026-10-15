package com.quickflow.learning;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LearningStatusCalculatorTest {

    @Test
    void noMilestonesGivesNoDerivedStatus() {
        assertThat(LearningStatusCalculator.statusFor(List.of())).isEmpty();
    }

    @Test
    void singleUndoneMilestoneIsNotStarted() {
        assertThat(LearningStatusCalculator.statusFor(List.of(false)))
                .contains(LearningStatus.NOT_STARTED);
    }

    @Test
    void singleDoneMilestoneIsCompleted() {
        assertThat(LearningStatusCalculator.statusFor(List.of(true)))
                .contains(LearningStatus.COMPLETED);
    }

    @Test
    void someDoneIsInProgress() {
        assertThat(LearningStatusCalculator.statusFor(List.of(true, false, false)))
                .contains(LearningStatus.IN_PROGRESS);
    }

    @Test
    void noneDoneAmongSeveralIsNotStarted() {
        assertThat(LearningStatusCalculator.statusFor(List.of(false, false, false)))
                .contains(LearningStatus.NOT_STARTED);
    }

    @Test
    void allDoneIsCompleted() {
        assertThat(LearningStatusCalculator.statusFor(List.of(true, true, true)))
                .contains(LearningStatus.COMPLETED);
    }

    @Test
    void oneUndoneAgainAfterAllDoneIsInProgress() {
        assertThat(LearningStatusCalculator.statusFor(List.of(true, false)))
                .contains(LearningStatus.IN_PROGRESS);
    }
}
