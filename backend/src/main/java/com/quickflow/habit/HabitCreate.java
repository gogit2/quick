package com.quickflow.habit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body of POST /api/habits (contract schema HabitCreate). */
public record HabitCreate(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull HabitFrequency frequency) {
}
