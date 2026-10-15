package com.quickflow.task;

import java.time.Instant;
import java.time.LocalDate;

/** Response body of the task operations (contract schema Task). */
public record Task(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        Instant createdAt,
        Instant updatedAt,
        boolean archived,
        boolean overdue) {
}
