package com.quickflow.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.quickflow.settings.Settings;
import com.quickflow.settings.SettingsService;
import com.quickflow.settings.StartPage;
import com.quickflow.support.MutableClock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class UserTimeTest {

    @Test
    void todayFollowsTheSavedZone() {
        UserTime utc = userTime("2026-10-14T23:30:00Z", "UTC");
        assertThat(utc.today()).isEqualTo(LocalDate.parse("2026-10-14"));

        // 11:00 UTC on 14 Oct is already 15 Oct at UTC+14 (Kiritimati)
        UserTime kiritimati = userTime("2026-10-14T11:00:00Z", "Pacific/Kiritimati");
        assertThat(kiritimati.today()).isEqualTo(LocalDate.parse("2026-10-15"));
        assertThat(kiritimati.zone()).isEqualTo(ZoneId.of("Pacific/Kiritimati"));
    }

    @Test
    void sameInstantGivesDifferentDaysInDifferentZones() {
        String instant = "2026-10-14T11:00:00Z";

        assertThat(userTime(instant, "UTC").today()).isEqualTo(LocalDate.parse("2026-10-14"));
        assertThat(userTime(instant, "Pacific/Kiritimati").today()).isEqualTo(LocalDate.parse("2026-10-15"));
    }

    @Test
    void weekRunsMondayToSundayAcrossTheSundayMidnightBoundary() {
        // Sunday 23:30 in UTC is already Monday 01:30 in Berlin (CEST, UTC+2)
        UserTime berlin = userTime("2026-10-18T23:30:00Z", "Europe/Berlin");
        assertThat(berlin.today()).isEqualTo(LocalDate.parse("2026-10-19"));
        assertThat(berlin.weekStart()).isEqualTo(LocalDate.parse("2026-10-19"));
        assertThat(berlin.weekEnd()).isEqualTo(LocalDate.parse("2026-10-25"));

        // The same instant in UTC is still Sunday, so the week started on the Monday before
        UserTime utc = userTime("2026-10-18T23:30:00Z", "UTC");
        assertThat(utc.today()).isEqualTo(LocalDate.parse("2026-10-18"));
        assertThat(utc.weekStart()).isEqualTo(LocalDate.parse("2026-10-12"));
        assertThat(utc.weekEnd()).isEqualTo(LocalDate.parse("2026-10-18"));
    }

    @Test
    void mondayIsTheFirstDayOfTheWeek() {
        UserTime monday = userTime("2026-10-12T00:00:00Z", "UTC");

        assertThat(monday.weekStart()).isEqualTo(LocalDate.parse("2026-10-12"));
        assertThat(monday.weekEnd()).isEqualTo(LocalDate.parse("2026-10-18"));
    }

    @Test
    void isFutureComparesWithTodayInTheUserZone() {
        UserTime userTime = userTime("2026-10-15T09:00:00Z", "UTC");

        assertThat(userTime.isFuture(LocalDate.parse("2026-10-16"))).isTrue();
        assertThat(userTime.isFuture(LocalDate.parse("2026-10-15"))).isFalse();
        assertThat(userTime.isFuture(LocalDate.parse("2026-10-14"))).isFalse();
    }

    private static UserTime userTime(String instant, String timeZone) {
        SettingsService settings = mock(SettingsService.class);
        when(settings.get()).thenReturn(new Settings("Alex", null, timeZone, true, StartPage.DASHBOARD, true));
        return new UserTime(new MutableClock(Instant.parse(instant)), settings);
    }
}
