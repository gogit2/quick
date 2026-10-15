package com.quickflow.plan;

import static org.assertj.core.api.Assertions.assertThat;

import com.quickflow.common.PlanItemSourceType;
import com.quickflow.habit.HabitCompletionRepository;
import com.quickflow.habit.HabitCreate;
import com.quickflow.habit.HabitFrequency;
import com.quickflow.habit.HabitRepository;
import com.quickflow.habit.HabitService;
import com.quickflow.learning.LearningCardCreate;
import com.quickflow.learning.LearningCardRepository;
import com.quickflow.learning.LearningService;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import com.quickflow.task.Task;
import com.quickflow.task.TaskCreate;
import com.quickflow.task.TaskRepository;
import com.quickflow.task.TaskService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

/** BR14 / D-5: deleting a source removes its plan items; plans left empty are deleted. */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/plan-item-cleanup-${random.uuid}")
class PlanItemCleanupTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final Instant END = Instant.parse("2026-10-15T10:00:00Z");

    @Autowired
    private PlanService service;

    @Autowired
    private PlanRepository plans;

    @Autowired
    private TaskService taskService;

    @Autowired
    private TaskRepository tasks;

    @Autowired
    private HabitService habitService;

    @Autowired
    private HabitRepository habitRepository;

    @Autowired
    private HabitCompletionRepository habitCompletions;

    @Autowired
    private LearningService learningService;

    @Autowired
    private LearningCardRepository cards;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetState() {
        plans.deleteAll();
        tasks.deleteAll();
        habitCompletions.deleteAll();
        habitRepository.deleteAll();
        cards.deleteAll();
        clock.set(START);
    }

    @Test
    void deletingATaskRemovesItsItemFromSeveralPlansAndDeletesThePlanLeftEmpty() {
        Task shared = newTask("Shared");
        Task other = newTask("Other");
        Plan onlyShared = service.create(plan("Only shared", shared.id()));
        Plan both = service.create(plan("Both", shared.id(), other.id()));

        taskService.delete(shared.id());

        assertThat(plans.existsById(onlyShared.id())).isFalse();
        Plan remaining = service.get(both.id());
        assertThat(remaining.items()).extracting(PlanItem::sourceId).containsExactly(other.id());
        assertThat(remaining.totalItems()).isEqualTo(1);
    }

    @Test
    void progressIsRecomputedAfterItemsAreRemoved() {
        Task done = newTask("Done");
        Task open = newTask("Open");
        Plan plan = service.create(plan("Mixed", done.id(), open.id()));
        service.setItemDone(plan.id(), itemIdOf(plan, done.id()), true);

        taskService.delete(open.id());

        Plan after = service.get(plan.id());
        assertThat(after.totalItems()).isEqualTo(1);
        assertThat(after.doneItems()).isEqualTo(1);
        assertThat(after.completionPercent()).isEqualTo(100);
    }

    @Test
    void deletingAHabitRemovesItsItem() {
        var habit = habitService.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        Plan plan = service.create(new PlanCreate("Habit plan", 30, START, END, 1,
                List.of(new PlanItemCreate(PlanItemSourceType.HABIT, habit.id()))));

        habitService.delete(habit.id());

        assertThat(plans.existsById(plan.id())).isFalse();
    }

    @Test
    void deletingALearningCardRemovesItsItemAndKeepsOtherItemsOfTheSamePlan() {
        var card = learningService.create(new LearningCardCreate("Spring course", null, null));
        Task task = newTask("Read chapter");
        Plan plan = service.create(new PlanCreate("Study", 60, START, END, 1, List.of(
                new PlanItemCreate(PlanItemSourceType.LEARNING_RESOURCE, card.id()),
                new PlanItemCreate(PlanItemSourceType.TASK, task.id()))));

        learningService.delete(card.id());

        Plan after = service.get(plan.id());
        assertThat(after.items()).extracting(PlanItem::sourceType).containsExactly(PlanItemSourceType.TASK);
    }

    @Test
    void archivedSourceKeepsItsItems() {
        Task task = newTask("Archive me");
        Plan plan = service.create(plan("Keep", task.id()));

        taskService.archive(task.id());

        assertThat(service.get(plan.id()).totalItems()).isEqualTo(1);
    }

    private Task newTask(String title) {
        return taskService.create(new TaskCreate(title, null, null, null, null));
    }

    private static PlanCreate plan(String title, long... taskIds) {
        List<PlanItemCreate> items = java.util.Arrays.stream(taskIds)
                .mapToObj(id -> new PlanItemCreate(PlanItemSourceType.TASK, id))
                .toList();
        return new PlanCreate(title, 60, START, START.plus(Duration.ofHours(1)), 1, items);
    }

    private static long itemIdOf(Plan plan, long taskId) {
        return plan.items().stream()
                .filter(i -> i.sourceId() == taskId)
                .findFirst()
                .orElseThrow()
                .id();
    }
}
