package com.quickflow.common;

import com.quickflow.settings.SettingsService;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Component;

/**
 * Dates in the user's time zone. "Now" comes from the injected Clock, the zone from the saved settings.
 * Plain logic: unit-testable with a MutableClock and a settings mock.
 */
@Component
public class UserTime {

    private final Clock clock;
    private final SettingsService settings;

    public UserTime(Clock clock, SettingsService settings) {
        this.clock = clock;
        this.settings = settings;
    }

    public ZoneId zone() {
        return ZoneId.of(settings.get().timeZone());
    }

    public LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), zone());
    }

    /** Monday of the current week. */
    public LocalDate weekStart() {
        return today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /** Sunday of the current week. */
    public LocalDate weekEnd() {
        return weekStart().plusDays(6);
    }

    public boolean isFuture(LocalDate date) {
        return date.isAfter(today());
    }
}
