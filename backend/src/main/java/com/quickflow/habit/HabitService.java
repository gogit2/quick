package com.quickflow.habit;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.ConflictException;
import com.quickflow.common.FieldError;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import com.quickflow.common.UserTime;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for habits and their completions. Time comes from the injected Clock; "today" from UserTime. */
@Service
public class HabitService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

    private final HabitRepository habits;
    private final HabitCompletionRepository completions;
    private final UserTime userTime;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public HabitService(HabitRepository habits, HabitCompletionRepository completions, UserTime userTime,
                        Clock clock, ApplicationEventPublisher events) {
        this.habits = habits;
        this.completions = completions;
        this.userTime = userTime;
        this.clock = clock;
        this.events = events;
    }

    /** Newest first (D-11). A null filter lists active and inactive habits. */
    @Transactional(readOnly = true)
    public List<Habit> list(Boolean active) {
        List<HabitEntity> found = active == null
                ? habits.findAll(NEWEST_FIRST)
                : habits.findByActive(active, NEWEST_FIRST);
        LocalDate today = userTime.today();
        return found.stream().map(h -> toDto(h, today)).toList();
    }

    @Transactional(readOnly = true)
    public Habit get(long id) {
        return toDto(load(id), userTime.today());
    }

    @Transactional
    public Habit create(HabitCreate in) {
        HabitEntity entity = new HabitEntity(in.name().trim(), in.description(), in.frequency(), Instant.now(clock));
        return toDto(habits.save(entity), userTime.today());
    }

    /** Full replacement. Completions are kept when the frequency changes. */
    @Transactional
    public Habit update(long id, HabitUpdate in) {
        HabitEntity entity = load(id);
        entity.setName(in.name().trim());
        entity.setDescription(in.description());
        entity.setFrequency(in.frequency());
        entity.setActive(in.active());
        return toDto(entity, userTime.today());
    }

    /** Deletes the habit with its completions and tells the plans so their items are removed (BR14, D-5). */
    @Transactional
    public void delete(long id) {
        HabitEntity entity = load(id);
        completions.deleteByHabitId(id);
        habits.delete(entity);
        events.publishEvent(new SourceDeletedEvent(PlanItemSourceType.HABIT, id));
    }

    /**
     * Records a completion for the date (today when null). Rules: no future date (400), no inactive habit (400),
     * one completion per date (BR7, 409), and one per Monday-Sunday week for a WEEKLY habit (D-6, 409).
     */
    @Transactional
    public HabitCompletion complete(long id, LocalDate requested) {
        HabitEntity habit = load(id);
        LocalDate date = requested == null ? userTime.today() : requested;
        if (userTime.isFuture(date)) {
            throw new BadRequestException("Completion date is in the future",
                    List.of(new FieldError("completionDate", "must not be in the future")));
        }
        if (!habit.isActive()) {
            throw new BadRequestException("Habit is inactive");
        }
        if (completions.existsByHabitIdAndCompletionDate(id, date)) {
            throw new ConflictException("Habit already completed for " + date);
        }
        if (habit.getFrequency() == HabitFrequency.WEEKLY) {
            LocalDate weekStart = HabitPeriods.periodStart(date, HabitFrequency.WEEKLY);
            boolean weekDone = completionDates(id).stream()
                    .anyMatch(d -> HabitPeriods.periodStart(d, HabitFrequency.WEEKLY).equals(weekStart));
            if (weekDone) {
                throw new ConflictException("Habit already completed in the week starting " + weekStart);
            }
        }
        HabitCompletionEntity saved = completions.save(new HabitCompletionEntity(id, date, Instant.now(clock)));
        return toDto(saved);
    }

    /** Completions newest first; from and to are inclusive and optional. */
    @Transactional(readOnly = true)
    public List<HabitCompletion> listCompletions(long id, LocalDate from, LocalDate to) {
        load(id);
        return completions.findByHabitIdOrderByCompletionDateDesc(id).stream()
                .filter(c -> from == null || !c.getCompletionDate().isBefore(from))
                .filter(c -> to == null || !c.getCompletionDate().isAfter(to))
                .map(HabitService::toDto)
                .toList();
    }

    /** Undo: removes the completion of one date. */
    @Transactional
    public void deleteCompletion(long id, LocalDate date) {
        load(id);
        HabitCompletionEntity completion = completions.findByHabitIdAndCompletionDate(id, date)
                .orElseThrow(() -> new NotFoundException("No completion of habit " + id + " for " + date));
        completions.delete(completion);
    }

    private HabitEntity load(long id) {
        return habits.findById(id)
                .orElseThrow(() -> new NotFoundException("Habit " + id + " not found"));
    }

    private Set<LocalDate> completionDates(long habitId) {
        return completions.findByHabitIdOrderByCompletionDateDesc(habitId).stream()
                .map(HabitCompletionEntity::getCompletionDate)
                .collect(Collectors.toSet());
    }

    /** Derived fields: done for the current period, current streak, total completions, period bounds. */
    private Habit toDto(HabitEntity entity, LocalDate today) {
        HabitFrequency frequency = entity.getFrequency();
        Set<LocalDate> dates = completionDates(entity.getId());
        LocalDate periodStart = HabitPeriods.periodStart(today, frequency);
        boolean doneForCurrentPeriod = dates.stream()
                .anyMatch(d -> HabitPeriods.periodStart(d, frequency).equals(periodStart));
        return new Habit(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                frequency,
                entity.getCreatedAt(),
                entity.isActive(),
                doneForCurrentPeriod,
                StreakCalculator.currentStreak(frequency, today, dates),
                dates.size(),
                periodStart,
                HabitPeriods.periodEnd(today, frequency));
    }

    private static HabitCompletion toDto(HabitCompletionEntity entity) {
        return new HabitCompletion(entity.getId(), entity.getHabitId(), entity.getCompletionDate(),
                entity.getCreatedAt());
    }
}
