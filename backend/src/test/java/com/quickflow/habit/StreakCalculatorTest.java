package com.quickflow.habit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    @Test
    void dailyStreakStopsAtAGap() {
        // Today, 14th and 13th are done; the 12th is missing, so the 11th does not count.
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2026, 10, 15),
                LocalDate.of(2026, 10, 14),
                LocalDate.of(2026, 10, 13),
                LocalDate.of(2026, 10, 11));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.DAILY, TODAY, dates)).isEqualTo(3);
    }

    @Test
    void notYetDoneTodayKeepsYesterdaysStreak() {
        Set<LocalDate> dates = Set.of(LocalDate.of(2026, 10, 14), LocalDate.of(2026, 10, 13));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.DAILY, TODAY, dates)).isEqualTo(2);
    }

    @Test
    void notDoneTodayAndYesterdayBreaksTheStreak() {
        Set<LocalDate> dates = Set.of(LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 12));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.DAILY, TODAY, dates)).isZero();
    }

    @Test
    void weeklyStreakCountsAcrossTheSundayMondayBoundary() {
        // Today is Monday 2026-10-19 (not done yet). Sunday 2026-10-18 closes the week of 10-12,
        // Saturday 2026-10-10 is in the week of 10-05; the week of 09-28 is empty.
        LocalDate today = LocalDate.of(2026, 10, 19);
        Set<LocalDate> dates = Set.of(LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 10));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.WEEKLY, today, dates)).isEqualTo(2);
    }

    @Test
    void weeklyStreakCountsAcrossTheYearBoundary() {
        // 2027-01-04 is a Monday. Sunday 2027-01-03 closes the week of 2026-12-28; Sunday 2026-12-27 is in the
        // week of 2026-12-21.
        LocalDate today = LocalDate.of(2027, 1, 4);
        Set<LocalDate> dates = Set.of(LocalDate.of(2027, 1, 3), LocalDate.of(2026, 12, 27));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.WEEKLY, today, dates)).isEqualTo(2);
    }

    @Test
    void dailyStreakCountsAcrossTheYearBoundary() {
        LocalDate today = LocalDate.of(2027, 1, 2);
        Set<LocalDate> dates = Set.of(
                LocalDate.of(2027, 1, 2),
                LocalDate.of(2027, 1, 1),
                LocalDate.of(2026, 12, 31));

        assertThat(StreakCalculator.currentStreak(HabitFrequency.DAILY, today, dates)).isEqualTo(3);
    }

    @Test
    void emptySetGivesZero() {
        assertThat(StreakCalculator.currentStreak(HabitFrequency.DAILY, TODAY, Set.of())).isZero();
        assertThat(StreakCalculator.currentStreak(HabitFrequency.WEEKLY, TODAY, Set.of())).isZero();
    }
}
