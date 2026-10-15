package com.quickflow.learning;

import java.util.Collection;
import java.util.Optional;

/** Status of a learning card derived from its milestones (D-7). Plain logic: no clock, no repository. */
public final class LearningStatusCalculator {

    private LearningStatusCalculator() {
    }

    /**
     * NOT_STARTED when no milestone is done, COMPLETED when all are, IN_PROGRESS otherwise.
     * Empty when there are no milestones: the card's status is then set by hand.
     */
    public static Optional<LearningStatus> statusFor(Collection<Boolean> doneFlags) {
        if (doneFlags.isEmpty()) {
            return Optional.empty();
        }
        long done = doneFlags.stream().filter(Boolean::booleanValue).count();
        if (done == 0) {
            return Optional.of(LearningStatus.NOT_STARTED);
        }
        if (done == doneFlags.size()) {
            return Optional.of(LearningStatus.COMPLETED);
        }
        return Optional.of(LearningStatus.IN_PROGRESS);
    }
}
