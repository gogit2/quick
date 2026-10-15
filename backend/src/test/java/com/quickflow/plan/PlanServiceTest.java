package com.quickflow.plan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickflow.common.BadRequestException;
import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.habit.Habit;
import com.quickflow.habit.HabitCompletionRepository;
import com.quickflow.habit.HabitCreate;
import com.quickflow.habit.HabitFrequency;
import com.quickflow.habit.HabitRepository;
import com.quickflow.habit.HabitService;
import com.quickflow.learning.LearningCard;
import com.quickflow.learning.LearningCardCreate;
import com.quickflow.learning.LearningCardRepository;
import com.quickflow.learning.LearningService;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import com.quickflow.task.Task;
import com.quickflow.task.TaskCreate;
import com.quickflow.task.TaskRepository;
import com.quickflow.task.TaskService;
import com.quickflow.task.TaskStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/** Business rules of PlanService on a real (file) H2 database with a controllable clock. */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/plan-service-${random.uuid}")
class PlanServiceTest {

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
    private HabitRepository habits;

    @Autowired
    private HabitCompletionRepository habitCompletions;

    @Autowired
    private LearningService learningService;

    @Autowired
    private LearningCardRepository cards;

    @Autowired
    private MutableClock clock;

    @Autowired
    private TransactionTemplate transactions;

    @BeforeEach
    void resetState() {
        plans.deleteAll();
        tasks.deleteAll();
        habitCompletions.deleteAll();
        habits.deleteAll();
        cards.deleteAll();
        clock.set(START.minus(Duration.ofHours(1)));
    }

    @Test
    void planWithoutItemsIsA400() {
        assertThatThrownBy(() -> service.create(create(START, END, 60, List.of())))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void endNotAfterStartIsA400() {
        assertThatThrownBy(() -> service.create(create(END, START, 60, List.of(taskItem(newTask().id())))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void unknownSourceIsA400() {
        assertThatThrownBy(() -> service.create(create(START, END, 60,
                List.of(new PlanItemCreate(PlanItemSourceType.TASK, 999_999L)))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void durationLongerThanWindowIsAcceptedWithWarning() {
        Plan plan = service.create(create(START, END, 90, List.of(taskItem(newTask().id()))));

        assertThat(plan.durationExceedsWindow()).isTrue();
        assertThat(plan.status()).isEqualTo(PlanStatus.NOT_STARTED);
    }

    @Test
    void listOrdersByPriorityAndFiltersByGroup() {
        Instant past = START.minus(Duration.ofDays(2));
        Plan second = service.create(new PlanCreate("Second", 30, START, END, 2, List.of(taskItem(newTask().id()))));
        Plan first = service.create(new PlanCreate("First", 30, START, END, 1, List.of(taskItem(newTask().id()))));
        Plan ended = service.create(new PlanCreate("Ended", 30, past, past.plus(Duration.ofHours(1)), 3,
                List.of(taskItem(newTask().id()))));

        assertThat(service.list(null)).extracting(Plan::id).containsExactly(first.id(), second.id(), ended.id());
        assertThat(service.list(PlanGroup.ACTIVE_UPCOMING)).extracting(Plan::id)
                .containsExactly(first.id(), second.id());
        assertThat(service.list(PlanGroup.COMPLETED)).extracting(Plan::id).containsExactly(ended.id());
    }

    @Test
    void tickingTaskItemSetsTaskDoneAndUnticking() {
        Task task = newTask();
        Plan plan = service.create(create(START, END, 60, List.of(taskItem(task.id()))));
        long itemId = plan.items().get(0).id();

        Plan ticked = service.setItemDone(plan.id(), itemId, true);

        assertThat(ticked.doneItems()).isEqualTo(1);
        assertThat(taskService.get(task.id()).status()).isEqualTo(TaskStatus.DONE);

        service.setItemDone(plan.id(), itemId, false);

        assertThat(taskService.get(task.id()).status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void tickingHabitAndLearningItemsChangesOnlyTheFlag() {
        Habit habit = habitService.create(new HabitCreate("Read", null, HabitFrequency.DAILY));
        LearningCard card = learningService.create(new LearningCardCreate("Spring course", null, null));
        Plan plan = service.create(create(START, END, 60, List.of(
                new PlanItemCreate(PlanItemSourceType.HABIT, habit.id()),
                new PlanItemCreate(PlanItemSourceType.LEARNING_RESOURCE, card.id()))));

        for (PlanItem item : plan.items()) {
            service.setItemDone(plan.id(), item.id(), true);
        }

        assertThat(habitCompletions.count()).isZero();
        assertThat(learningService.get(card.id()).status()).isEqualTo(card.status());
        assertThat(service.get(plan.id()).doneItems()).isEqualTo(2);
    }

    @Test
    void progressUpdatesImmediately() {
        Plan plan = service.create(create(START, END, 60, List.of(
                taskItem(newTask().id()), taskItem(newTask().id()), taskItem(newTask().id()))));

        Plan afterOne = service.setItemDone(plan.id(), plan.items().get(0).id(), true);

        assertThat(afterOne.doneItems()).isEqualTo(1);
        assertThat(afterOne.completionPercent()).isEqualTo(33);
    }

    @Test
    void statusFollowsTheClockAcrossStartAndEnd() {
        Plan plan = service.create(create(START, END, 30, List.of(taskItem(newTask().id()))));

        assertThat(service.get(plan.id()).status()).isEqualTo(PlanStatus.NOT_STARTED);

        clock.set(START);
        Plan running = service.get(plan.id());
        assertThat(running.status()).isEqualTo(PlanStatus.IN_PROGRESS);
        assertThat(running.restSeconds()).isEqualTo(3600L);

        clock.set(END);
        Plan ended = service.get(plan.id());
        assertThat(ended.status()).isEqualTo(PlanStatus.COMPLETED);
        assertThat(ended.restSeconds()).isNull();
    }

    @Test
    void statusIsRecomputedByANewServiceInstance() {
        Plan plan = service.create(create(START, END, 30, List.of(taskItem(newTask().id()))));
        clock.set(START.plus(Duration.ofMinutes(10)));

        PlanService restarted = new PlanService(plans, tasks, habits, cards, taskService, clock);

        Plan afterRestart = transactions.execute(status -> restarted.get(plan.id()));

        assertThat(afterRestart).isEqualTo(service.get(plan.id()));
        assertThat(afterRestart.status()).isEqualTo(PlanStatus.IN_PROGRESS);
    }

    @Test
    void acknowledgeStartIsIdempotent() {
        Plan plan = service.create(create(START, END, 30, List.of(taskItem(newTask().id()))));

        assertThat(service.acknowledgeStart(plan.id()).startNotified()).isTrue();
        assertThat(service.acknowledgeStart(plan.id()).startNotified()).isTrue();
    }

    @Test
    void deletingAPlanLeavesItsSourcesUntouched() {
        Task task = newTask();
        Plan plan = service.create(create(START, END, 30, List.of(taskItem(task.id()))));

        service.delete(plan.id());

        assertThat(tasks.existsById(task.id())).isTrue();
        assertThatThrownBy(() -> service.get(plan.id())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void unknownPlanOrItemIsNotFound() {
        Plan plan = service.create(create(START, END, 30, List.of(taskItem(newTask().id()))));

        assertThatThrownBy(() -> service.setItemDone(plan.id(), 999_999L, true))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.get(999_999L)).isInstanceOf(NotFoundException.class);
    }

    private Task newTask() {
        return taskService.create(new TaskCreate("Write report", null, null, null, null));
    }

    private static PlanItemCreate taskItem(long taskId) {
        return new PlanItemCreate(PlanItemSourceType.TASK, taskId);
    }

    private static PlanCreate create(Instant start, Instant end, int minutes, List<PlanItemCreate> items) {
        return new PlanCreate("Focused morning", minutes, start, end, 1, items);
    }
}
