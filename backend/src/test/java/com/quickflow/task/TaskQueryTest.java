package com.quickflow.task;

import static org.assertj.core.api.Assertions.assertThat;

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

/**
 * Search, filter and sort of TaskService.list. Today is 2026-10-15 (Thursday), so this week is 2026-10-12..2026-10-18.
 * Seed (creation order, one minute apart): A "Write report" HIGH TODO due 10-20; B "Buy milk" LOW TODO due 10-15;
 * C "Pay rent" MEDIUM TODO due 10-14; D "Call dentist" MEDIUM IN_PROGRESS no due date; E "Review plan" HIGH DONE
 * due 10-12; F "Archive me" LOW TODO due 10-01 and archived.
 */
@SpringBootTest
@Import(MutableClockTestConfig.class)
@TestPropertySource(properties = "qf.db.path=./target/test-data/task-query-${random.uuid}")
class TaskQueryTest {

    private static final Instant START = Instant.parse("2026-10-15T09:00:00Z");

    @Autowired
    private TaskService service;

    @Autowired
    private TaskRepository repository;

    @Autowired
    private MutableClock clock;

    private long a;
    private long b;
    private long c;
    private long d;
    private long e;
    private long f;

    @BeforeEach
    void seed() {
        repository.deleteAll();
        clock.set(START);
        a = create("Write report", TaskStatus.TODO, TaskPriority.HIGH, LocalDate.of(2026, 10, 20));
        b = create("Buy milk", TaskStatus.TODO, TaskPriority.LOW, LocalDate.of(2026, 10, 15));
        c = create("Pay rent", TaskStatus.TODO, TaskPriority.MEDIUM, LocalDate.of(2026, 10, 14));
        d = create("Call dentist", TaskStatus.IN_PROGRESS, TaskPriority.MEDIUM, null);
        e = create("Review plan", TaskStatus.DONE, TaskPriority.HIGH, LocalDate.of(2026, 10, 12));
        f = create("Archive me", TaskStatus.TODO, TaskPriority.LOW, LocalDate.of(2026, 10, 1));
        service.archive(f);
    }

    @Test
    void searchMatchesTitleCaseInsensitivePartial() {
        assertThat(ids(query(q("REPORT")))).containsExactly(a);
        assertThat(ids(query(q("port")))).containsExactly(a);
        assertThat(ids(query(q("zebra")))).isEmpty();
    }

    @Test
    void statusFilter() {
        assertThat(ids(query(status(TaskStatus.IN_PROGRESS)))).containsExactly(d);
    }

    @Test
    void statusAndPriorityFiltersCombine() {
        TaskQuery query = new TaskQuery(null, TaskStatus.TODO, TaskPriority.HIGH, null, null, null, false, null);

        assertThat(ids(query(query))).containsExactly(a);
    }

    @Test
    void dueTodayFilter() {
        assertThat(ids(query(due(TaskDueFilter.TODAY)))).containsExactly(b);
    }

    @Test
    void overdueFilterExcludesDoneAndArchived() {
        assertThat(ids(query(due(TaskDueFilter.OVERDUE)))).containsExactly(c);
    }

    @Test
    void thisWeekFilterIsMondayToSunday() {
        assertThat(ids(query(due(TaskDueFilter.THIS_WEEK)))).containsExactly(e, c, b);
    }

    @Test
    void noDueDateFilter() {
        assertThat(ids(query(due(TaskDueFilter.NO_DUE_DATE)))).containsExactly(d);
    }

    @Test
    void dueRangeIsInclusive() {
        TaskQuery query = new TaskQuery(null, null, null, null,
                LocalDate.of(2026, 10, 15), LocalDate.of(2026, 10, 20), false, null);

        assertThat(ids(query(query))).containsExactly(b, a);
    }

    @Test
    void archivedTrueListsArchivedOnly() {
        TaskQuery query = new TaskQuery(null, null, null, null, null, null, true, null);

        assertThat(ids(query(query))).containsExactly(f);
    }

    @Test
    void defaultSortIsCreatedNewestFirst() {
        assertThat(ids(query(sort(null)))).containsExactly(e, d, c, b, a);
    }

    @Test
    void createdAscSort() {
        assertThat(ids(query(sort(TaskSort.CREATED_ASC)))).containsExactly(a, b, c, d, e);
    }

    @Test
    void dueDateAscSortPutsNullDueDatesLast() {
        assertThat(ids(query(sort(TaskSort.DUE_DATE_ASC)))).containsExactly(e, c, b, a, d);
    }

    @Test
    void dueDateDescSortPutsNullDueDatesLast() {
        assertThat(ids(query(sort(TaskSort.DUE_DATE_DESC)))).containsExactly(a, b, c, e, d);
    }

    private long create(String title, TaskStatus status, TaskPriority priority, LocalDate dueDate) {
        clock.advance(Duration.ofMinutes(1));
        return service.create(new TaskCreate(title, null, status, priority, dueDate)).id();
    }

    private List<Task> query(TaskQuery query) {
        return service.list(query);
    }

    private static TaskQuery q(String text) {
        return new TaskQuery(text, null, null, null, null, null, false, null);
    }

    private static TaskQuery status(TaskStatus status) {
        return new TaskQuery(null, status, null, null, null, null, false, null);
    }

    private static TaskQuery due(TaskDueFilter due) {
        return new TaskQuery(null, null, null, due, null, null, false, null);
    }

    private static TaskQuery sort(TaskSort sort) {
        return new TaskQuery(null, null, null, null, null, null, false, sort);
    }

    private static List<Long> ids(List<Task> tasks) {
        return tasks.stream().map(Task::id).toList();
    }
}
