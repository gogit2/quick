package com.quickflow.habit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.ConflictException;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * Business rules of HabitService on a real (file) H2 database with a controllable clock.
 * Today is Thursday 2026-10-15 (UTC); the week is Monday 2026-10-12 to Sunday 2026-10-18.
 */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@RecordApplicationEvents
@TestPropertySource(properties = "qf.db.path=./target/test-data/habit-service-${random.uuid}")
class HabitServiceTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Autowired
    private HabitService service;

    @Autowired
    private HabitRepository habits;

    @Autowired
    private HabitCompletionRepository completions;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetState() {
        completions.deleteAll();
        habits.deleteAll();
        clock.set(START);
    }

    @Test
    void newDailyHabitIsNotDoneAndHasNoStreak() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));

        assertThat(habit.active()).isTrue();
        assertThat(habit.doneForCurrentPeriod()).isFalse();
        assertThat(habit.currentStreak()).isZero();
        assertThat(habit.totalCompletions()).isZero();
        assertThat(habit.periodStart()).isEqualTo(TODAY);
        assertThat(habit.periodEnd()).isEqualTo(TODAY);
    }

    @Test
    void weeklyHabitPeriodIsTheWholeMondayToSundayWeek() {
        Habit habit = service.create(new HabitCreate("Review", "weekly", HabitFrequency.WEEKLY));

        assertThat(habit.periodStart()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(habit.periodEnd()).isEqualTo(LocalDate.of(2026, 10, 18));
    }

    @Test
    void completingForTodayMarksDoneAndCountsOne() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));

        HabitCompletion completion = service.complete(habit.id(), null);

        assertThat(completion.completionDate()).isEqualTo(TODAY);
        assertThat(completion.habitId()).isEqualTo(habit.id());
        Habit after = service.get(habit.id());
        assertThat(after.doneForCurrentPeriod()).isTrue();
        assertThat(after.totalCompletions()).isEqualTo(1);
        assertThat(after.currentStreak()).isEqualTo(1);
    }

    @Test
    void sameDateTwiceIsAConflictAndTheDatabaseKeepsItUnique() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        service.complete(habit.id(), TODAY);

        assertThatThrownBy(() -> service.complete(habit.id(), TODAY))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Habit already completed for 2026-10-15");
        assertThatThrownBy(() -> completions.saveAndFlush(
                new HabitCompletionEntity(habit.id(), TODAY, START)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void futureDateIsA400() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));

        assertThatThrownBy(() -> service.complete(habit.id(), TODAY.plusDays(1)))
                .isInstanceOf(BadRequestException.class)
                .satisfies(e -> assertThat(((BadRequestException) e).getFieldErrors())
                        .extracting(f -> f.field())
                        .containsExactly("completionDate"));
    }

    @Test
    void inactiveHabitCannotBeCompletedButKeepsItsData() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        service.complete(habit.id(), YESTERDAY);

        Habit deactivated = service.update(habit.id(), new HabitUpdate("Read", null, HabitFrequency.DAILY, false));

        assertThat(deactivated.active()).isFalse();
        assertThat(deactivated.totalCompletions()).isEqualTo(1);
        assertThatThrownBy(() -> service.complete(habit.id(), null))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Habit is inactive");
    }

    @Test
    void weeklyHabitAcceptsOneCompletionPerWeek() {
        Habit habit = service.create(new HabitCreate("Review", null, HabitFrequency.WEEKLY));
        service.complete(habit.id(), LocalDate.of(2026, 10, 12));

        assertThatThrownBy(() -> service.complete(habit.id(), TODAY))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Habit already completed in the week starting 2026-10-12");

        // The Sunday before belongs to the previous week, so it is allowed.
        service.complete(habit.id(), LocalDate.of(2026, 10, 11));
        assertThat(service.get(habit.id()).totalCompletions()).isEqualTo(2);
    }

    @Test
    void completionOfYesterdayRaisesTotalAndStreakAndTodayExtendsIt() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));

        service.complete(habit.id(), YESTERDAY);
        Habit afterYesterday = service.get(habit.id());
        assertThat(afterYesterday.totalCompletions()).isEqualTo(1);
        assertThat(afterYesterday.currentStreak()).isEqualTo(1);
        assertThat(afterYesterday.doneForCurrentPeriod()).isFalse();

        service.complete(habit.id(), null);
        Habit afterToday = service.get(habit.id());
        assertThat(afterToday.totalCompletions()).isEqualTo(2);
        assertThat(afterToday.currentStreak()).isEqualTo(2);
        assertThat(afterToday.doneForCurrentPeriod()).isTrue();
    }

    @Test
    void completionsAreListedNewestFirstAndFilteredByRange() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        service.complete(habit.id(), LocalDate.of(2026, 10, 10));
        service.complete(habit.id(), LocalDate.of(2026, 10, 13));
        service.complete(habit.id(), TODAY);

        assertThat(service.listCompletions(habit.id(), null, null))
                .extracting(HabitCompletion::completionDate)
                .containsExactly(TODAY, LocalDate.of(2026, 10, 13), LocalDate.of(2026, 10, 10));
        assertThat(service.listCompletions(habit.id(), LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 14)))
                .extracting(HabitCompletion::completionDate)
                .containsExactly(LocalDate.of(2026, 10, 13));
    }

    @Test
    void undoRemovesTheCompletionOfThatDate() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        service.complete(habit.id(), null);

        service.deleteCompletion(habit.id(), TODAY);

        assertThat(service.get(habit.id()).doneForCurrentPeriod()).isFalse();
        assertThatThrownBy(() -> service.deleteCompletion(habit.id(), TODAY))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listFiltersByActiveAndKeepsDeactivatedHabits() {
        Habit active = service.create(new HabitCreate("Active one", null, HabitFrequency.DAILY));
        Habit paused = service.create(new HabitCreate("Paused one", null, HabitFrequency.DAILY));
        service.update(paused.id(), new HabitUpdate("Paused one", null, HabitFrequency.DAILY, false));

        assertThat(service.list(null)).hasSize(2);
        assertThat(service.list(true)).extracting(Habit::id).containsExactly(active.id());
        assertThat(service.list(false)).extracting(Habit::id).containsExactly(paused.id());
    }

    @Test
    void listIsNewestFirst() {
        Habit first = service.create(new HabitCreate("First", null, HabitFrequency.DAILY));
        clock.advance(Duration.ofMinutes(5));
        Habit second = service.create(new HabitCreate("Second", null, HabitFrequency.DAILY));

        assertThat(service.list(null)).extracting(Habit::id).containsExactly(second.id(), first.id());
    }

    @Test
    void deleteRemovesTheHabitAndItsCompletionsAndPublishesAnEvent() {
        Habit habit = service.create(new HabitCreate("Gone soon", null, HabitFrequency.DAILY));
        service.complete(habit.id(), YESTERDAY);

        service.delete(habit.id());

        assertThat(events.stream(SourceDeletedEvent.class))
                .containsExactly(new SourceDeletedEvent(PlanItemSourceType.HABIT, habit.id()));
        assertThat(completions.findByHabitIdOrderByCompletionDateDesc(habit.id())).isEmpty();
        assertThatThrownBy(() -> service.get(habit.id()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Habit " + habit.id() + " not found");
    }

    @Test
    void updateReplacesTheFieldsAndTrimsTheName() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));

        Habit updated = service.update(habit.id(),
                new HabitUpdate("  Read more  ", "20 pages", HabitFrequency.WEEKLY, true));

        assertThat(updated.name()).isEqualTo("Read more");
        assertThat(updated.description()).isEqualTo("20 pages");
        assertThat(updated.frequency()).isEqualTo(HabitFrequency.WEEKLY);
        assertThat(updated.createdAt()).isEqualTo(START);
    }

    @Test
    void completionsOfOtherHabitsDoNotCount() {
        Habit habit = service.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        Habit other = service.create(new HabitCreate("Run", null, HabitFrequency.DAILY));
        service.complete(other.id(), null);

        Habit after = service.get(habit.id());
        assertThat(after.doneForCurrentPeriod()).isFalse();
        assertThat(after.totalCompletions()).isZero();
        assertThat(service.get(other.id()).totalCompletions()).isEqualTo(1);
    }
}
