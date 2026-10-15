package com.quickflow.task;

import java.time.LocalDate;

/** Entity to response mapping. Plain class: overdue is computed from the entity and today's date. */
public final class TaskMapper {

    private TaskMapper() {
    }

    public static Task toDto(TaskEntity e, LocalDate today) {
        return new Task(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getStatus(),
                e.getPriority(),
                e.getDueDate(),
                e.getCreatedAt(),
                e.getUpdatedAt(),
                e.isArchived(),
                isOverdue(e, today));
    }

    /** Due before today (user time zone), not DONE and not archived (FR-02). */
    public static boolean isOverdue(TaskEntity e, LocalDate today) {
        return e.getDueDate() != null
                && e.getDueDate().isBefore(today)
                && e.getStatus() != TaskStatus.DONE
                && !e.isArchived();
    }
}
