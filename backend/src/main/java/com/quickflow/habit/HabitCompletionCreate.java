package com.quickflow.habit;

import java.time.LocalDate;

/** Request body of POST /api/habits/{id}/completions (contract schema HabitCompletionCreate). Null means today. */
public record HabitCompletionCreate(LocalDate completionDate) {
}
