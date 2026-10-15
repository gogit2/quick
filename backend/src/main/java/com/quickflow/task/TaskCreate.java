package com.quickflow.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Request body of POST /api/tasks (contract schema TaskCreate). Defaults are applied by TaskService. */
public record TaskCreate(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 2000) String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate) {
}
