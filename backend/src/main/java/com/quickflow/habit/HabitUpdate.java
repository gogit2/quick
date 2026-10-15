package com.quickflow.habit;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request body of PUT /api/habits/{id} (contract schema HabitUpdate). Full replacement; active=false deactivates. */
public record HabitUpdate(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 2000) String description,
        @NotNull HabitFrequency frequency,
        @NotNull Boolean active) {
}
