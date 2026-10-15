package com.quickflow.habit;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * The period a habit is counted in: the day (DAILY) or the Monday-Sunday week (WEEKLY, D-6).
 * Plain logic, no clock: callers pass the dates.
 */
public final class HabitPeriods {

    private HabitPeriods() {
    }

    /** First day of the period that contains the date. */
    public static LocalDate periodStart(LocalDate date, HabitFrequency frequency) {
        return frequency == HabitFrequency.WEEKLY
                ? date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                : date;
    }

    /** Last day of the period that contains the date. */
    public static LocalDate periodEnd(LocalDate date, HabitFrequency frequency) {
        return frequency == HabitFrequency.WEEKLY
                ? periodStart(date, frequency).plusDays(6)
                : date;
    }

    /** Start of the period just before the one that starts on periodStart. */
    public static LocalDate previous(LocalDate periodStart, HabitFrequency frequency) {
        return frequency == HabitFrequency.WEEKLY
                ? periodStart.minusWeeks(1)
                : periodStart.minusDays(1);
    }
}
