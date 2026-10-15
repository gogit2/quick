package com.quickflow.habit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class HabitPeriodsTest {

    @Test
    void dailyPeriodIsTheDay() {
        LocalDate day = LocalDate.of(2026, 10, 15);

        assertThat(HabitPeriods.periodStart(day, HabitFrequency.DAILY)).isEqualTo(day);
        assertThat(HabitPeriods.periodEnd(day, HabitFrequency.DAILY)).isEqualTo(day);
    }

    @Test
    void weeklyPeriodRunsMondayToSundayIncludingSunday() {
        // 2026-10-18 is a Sunday: it belongs to the week that started on Monday 2026-10-12.
        LocalDate sunday = LocalDate.of(2026, 10, 18);

        assertThat(HabitPeriods.periodStart(sunday, HabitFrequency.WEEKLY)).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(HabitPeriods.periodEnd(sunday, HabitFrequency.WEEKLY)).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void mondayStartsANewWeek() {
        LocalDate monday = LocalDate.of(2026, 10, 19);

        assertThat(HabitPeriods.periodStart(monday, HabitFrequency.WEEKLY)).isEqualTo(monday);
        assertThat(HabitPeriods.periodEnd(monday, HabitFrequency.WEEKLY)).isEqualTo(LocalDate.of(2026, 10, 25));
    }

    @Test
    void weekCrossesTheYearBoundary() {
        // 2027-01-01 is a Friday: its week runs Monday 2026-12-28 to Sunday 2027-01-03.
        LocalDate newYear = LocalDate.of(2027, 1, 1);

        assertThat(HabitPeriods.periodStart(newYear, HabitFrequency.WEEKLY)).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(HabitPeriods.periodEnd(newYear, HabitFrequency.WEEKLY)).isEqualTo(LocalDate.of(2027, 1, 3));
    }

    @Test
    void previousPeriodStepsByOneDayOrOneWeek() {
        assertThat(HabitPeriods.previous(LocalDate.of(2026, 10, 1), HabitFrequency.DAILY))
                .isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(HabitPeriods.previous(LocalDate.of(2026, 10, 12), HabitFrequency.WEEKLY))
                .isEqualTo(LocalDate.of(2026, 10, 5));
    }
}
