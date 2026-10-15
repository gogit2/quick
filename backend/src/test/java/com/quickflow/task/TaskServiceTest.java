package com.quickflow.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quickflow.common.NotFoundException;
import com.quickflow.common.PlanItemSourceType;
import com.quickflow.common.SourceDeletedEvent;
import com.quickflow.support.MutableClock;
import com.quickflow.support.MutableClockTestConfig;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/** Business rules of TaskService on a real (file) H2 database with a controllable clock. Today is 2026-10-15 (UTC). */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@RecordApplicationEvents
@TestPropertySource(properties = "qf.db.path=./target/test-data/task-service-${random.uuid}")
class TaskServiceTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final TaskQuery DEFAULT_LIST = new TaskQuery(null, null, null, null, null, null, false, null);
    private static final TaskQuery ARCHIVED_LIST = new TaskQuery(null, null, null, null, null, null, true, null);

    @Autowired
    private TaskService service;

    @Autowired
    private TaskRepository repository;

    @Autowired
    private MutableClock clock;

    @Autowired
    private ApplicationEvents events;

    @BeforeEach
    void resetState() {
        repository.deleteAll();
        clock.set(START);
    }

    @Test
    void newTaskDefaultsToTodoAndMedium() {
        Task task = service.create(new TaskCreate("Write report", null, null, null, null));

        assertThat(task.status()).isEqualTo(TaskStatus.TODO);
        assertThat(task.priority()).isEqualTo(TaskPriority.MEDIUM);
        assertThat(task.archived()).isFalse();
        assertThat(task.overdue()).isFalse();
        assertThat(task.createdAt()).isEqualTo(START);
        assertThat(task.updatedAt()).isEqualTo(START);
    }

    @Test
    void completeSetsStatusDone() {
        Task task = service.create(new TaskCreate("Write report", null, null, null, null));

        assertThat(service.complete(task.id()).status()).isEqualTo(TaskStatus.DONE);
        assertThat(service.get(task.id()).status()).isEqualTo(TaskStatus.DONE);
    }

    @Test
    void archivedTasksAreExcludedFromDefaultListAndListedWhenArchivedTrue() {
        Task task = service.create(new TaskCreate("Old idea", null, null, null, null));

        service.archive(task.id());

        assertThat(service.list(DEFAULT_LIST)).isEmpty();
        List<Task> archived = service.list(ARCHIVED_LIST);
        assertThat(archived).hasSize(1);
        assertThat(archived.get(0).archived()).isTrue();
    }

    @Test
    void restoreReturnsTaskToDefaultList() {
        Task task = service.create(new TaskCreate("Old idea", null, null, null, null));
        service.archive(task.id());

        Task restored = service.restore(task.id());

        assertThat(restored.archived()).isFalse();
        assertThat(service.list(DEFAULT_LIST)).extracting(Task::id).containsExactly(task.id());
    }

    @Test
    void overdueOnlyWhenDueBeforeTodayAndNotDoneAndNotArchived() {
        Task overdue = service.create(new TaskCreate("Pay rent", null, null, null, TODAY.minusDays(1)));
        Task dueToday = service.create(new TaskCreate("Buy milk", null, null, null, TODAY));
        Task doneLate = service.create(new TaskCreate("Filed taxes", null, null, null, TODAY.minusDays(1)));
        Task archivedLate = service.create(new TaskCreate("Ancient", null, null, null, TODAY.minusDays(1)));
        service.complete(doneLate.id());
        service.archive(archivedLate.id());

        assertThat(service.get(overdue.id()).overdue()).isTrue();
        assertThat(service.get(dueToday.id()).overdue()).isFalse();
        assertThat(service.get(doneLate.id()).overdue()).isFalse();
        assertThat(service.get(archivedLate.id()).overdue()).isFalse();
    }

    @Test
    void updateReplacesFieldsAndChangesUpdatedAt() {
        Task task = service.create(new TaskCreate("Draft", "first", TaskStatus.IN_PROGRESS, TaskPriority.HIGH, TODAY));
        clock.advance(Duration.ofHours(1));

        Task updated = service.update(task.id(),
                new TaskUpdate("Final", null, TaskStatus.DONE, TaskPriority.LOW, null));

        assertThat(updated.title()).isEqualTo("Final");
        assertThat(updated.description()).isNull();
        assertThat(updated.status()).isEqualTo(TaskStatus.DONE);
        assertThat(updated.priority()).isEqualTo(TaskPriority.LOW);
        assertThat(updated.dueDate()).isNull();
        assertThat(updated.createdAt()).isEqualTo(START);
        assertThat(updated.updatedAt()).isEqualTo(START.plus(Duration.ofHours(1)));
    }

    @Test
    void updateKeepsStatusAndPriorityWhenMissing() {
        Task task = service.create(new TaskCreate("Draft", null, TaskStatus.IN_PROGRESS, TaskPriority.HIGH, null));

        Task updated = service.update(task.id(), new TaskUpdate("Draft v2", null, null, null, null));

        assertThat(updated.status()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.priority()).isEqualTo(TaskPriority.HIGH);
    }

    @Test
    void reopenSetsStatusTodo() {
        Task task = service.create(new TaskCreate("Draft", null, null, null, null));
        service.complete(task.id());

        service.reopen(task.id());

        assertThat(service.get(task.id()).status()).isEqualTo(TaskStatus.TODO);
    }

    @Test
    void deletePublishesSourceDeletedEventAndRemovesTask() {
        Task task = service.create(new TaskCreate("Gone soon", null, null, null, null));

        service.delete(task.id());

        assertThat(events.stream(SourceDeletedEvent.class))
                .containsExactly(new SourceDeletedEvent(PlanItemSourceType.TASK, task.id()));
        assertThatThrownBy(() -> service.get(task.id()))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Task " + task.id() + " not found");
    }
}
