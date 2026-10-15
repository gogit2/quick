package com.quickflow.plan;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.FieldError;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.habit.HabitEntity;
import com.quickflow.habit.HabitRepository;
import com.quickflow.learning.LearningCardEntity;
import com.quickflow.learning.LearningCardRepository;
import com.quickflow.task.TaskEntity;
import com.quickflow.task.TaskRepository;
import com.quickflow.task.TaskService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Business rules for plans: creation, listing, item progress and the start notice. Time comes from the Clock. */
@Service
public class PlanService {

    private final PlanRepository plans;
    private final TaskRepository tasks;
    private final HabitRepository habits;
    private final LearningCardRepository cards;
    private final TaskService taskService;
    private final Clock clock;

    public PlanService(PlanRepository plans, TaskRepository tasks, HabitRepository habits,
                       LearningCardRepository cards, TaskService taskService, Clock clock) {
        this.plans = plans;
        this.tasks = tasks;
        this.habits = habits;
        this.cards = cards;
        this.taskService = taskService;
        this.clock = clock;
    }

    /**
     * BR10: at least one item. BR11: end after start. Every source must exist and an item may appear only once.
     * A duration longer than the window is accepted (D-9).
     */
    @Transactional
    public Plan create(PlanCreate in) {
        if (in.items() == null || in.items().isEmpty()) {
            throw new BadRequestException("Validation failed",
                    List.of(new FieldError("items", "must contain at least one item")));
        }
        if (!in.endDateTime().isAfter(in.startDateTime())) {
            throw new BadRequestException("Validation failed",
                    List.of(new FieldError("endDateTime", "must be after startDateTime")));
        }
        PlanEntity plan = new PlanEntity(in.title().trim(), in.estimatedDurationMinutes(), in.startDateTime(),
                in.endDateTime(), in.priorityOrder(), Instant.now(clock));
        Set<List<?>> seen = new HashSet<>();
        for (PlanItemCreate item : in.items()) {
            if (!seen.add(List.of(item.sourceType(), item.sourceId()))) {
                throw new BadRequestException("Validation failed",
                        List.of(new FieldError("items", "duplicate item " + item.sourceType() + " " + item.sourceId())));
            }
            if (!sourceExists(item.sourceType(), item.sourceId())) {
                throw new BadRequestException("Validation failed",
                        List.of(new FieldError("items", item.sourceType() + " " + item.sourceId() + " not found")));
            }
            plan.addItem(new PlanItemEntity(item.sourceType(), item.sourceId()));
        }
        return toDto(plans.saveAndFlush(plan), Instant.now(clock));
    }

    /** ACTIVE_UPCOMING = NOT_STARTED + IN_PROGRESS; COMPLETED = COMPLETED; null = all. Ordered by priority, then start. */
    @Transactional(readOnly = true)
    public List<Plan> list(PlanGroup group) {
        Instant now = Instant.now(clock);
        return plans.findAll(Sort.by("priorityOrder", "startDateTime")).stream()
                .map(p -> toDto(p, now))
                .filter(p -> group == null || inGroup(p.status(), group))
                .toList();
    }

    @Transactional(readOnly = true)
    public Plan get(long id) {
        return toDto(load(id), Instant.now(clock));
    }

    /** Removes the plan only; its sources are untouched. */
    @Transactional
    public void delete(long id) {
        plans.delete(load(id));
    }

    /**
     * Ticks or unticks one item. A TASK item also sets its task DONE (done=true) or TODO (done=false) (D-3, BR13).
     * HABIT and LEARNING_RESOURCE items change only the flag.
     */
    @Transactional
    public Plan setItemDone(long planId, long itemId, boolean done) {
        PlanEntity plan = load(planId);
        PlanItemEntity item = plan.findItem(itemId)
                .orElseThrow(() -> new NotFoundException("Plan item " + itemId + " not found"));
        item.setDone(done);
        if (item.getSourceType() == PlanItemSourceType.TASK) {
            if (done) {
                taskService.complete(item.getSourceId());
            } else {
                taskService.reopen(item.getSourceId());
            }
        }
        return toDto(plan, Instant.now(clock));
    }

    /** Idempotent: records that the start notice was shown. */
    @Transactional
    public Plan acknowledgeStart(long planId) {
        PlanEntity plan = load(planId);
        plan.setStartNotified(true);
        return toDto(plan, Instant.now(clock));
    }

    private static boolean inGroup(PlanStatus status, PlanGroup group) {
        return group == PlanGroup.COMPLETED ? status == PlanStatus.COMPLETED : status != PlanStatus.COMPLETED;
    }

    private boolean sourceExists(PlanItemSourceType type, long id) {
        return switch (type) {
            case TASK -> tasks.existsById(id);
            case HABIT -> habits.existsById(id);
            case LEARNING_RESOURCE -> cards.existsById(id);
        };
    }

    private String titleOf(PlanItemEntity item) {
        long id = item.getSourceId();
        return switch (item.getSourceType()) {
            case TASK -> tasks.findById(id).map(TaskEntity::getTitle).orElse("");
            case HABIT -> habits.findById(id).map(HabitEntity::getName).orElse("");
            case LEARNING_RESOURCE -> cards.findById(id).map(LearningCardEntity::getTitle).orElse("");
        };
    }

    private PlanEntity load(long id) {
        return plans.findById(id)
                .orElseThrow(() -> new NotFoundException("Plan " + id + " not found"));
    }

    private Plan toDto(PlanEntity plan, Instant now) {
        return PlanMapper.toDto(plan, now, this::titleOf);
    }
}
