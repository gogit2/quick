package com.quickflow.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Request body of PUT /api/tasks/{id} (contract schema TaskUpdate). Full replacement: a missing status or
 * priority keeps the current value, a missing description or due date clears it.
 */
public record TaskUpdate(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {
}
