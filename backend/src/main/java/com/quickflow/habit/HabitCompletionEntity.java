package com.quickflow.habit;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One completion of a habit for a calendar date (table habit_completion). */
@Entity
@Table(name = "habit_completion")
public class HabitCompletionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long habitId;

    private LocalDate completionDate;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;

    protected HabitCompletionEntity() {
    }

    public HabitCompletionEntity(Long habitId, LocalDate completionDate, Instant now) {
        this.habitId = habitId;
        this.completionDate = completionDate;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public Long getHabitId() {
        return habitId;
    }

    public LocalDate getCompletionDate() {
        return completionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
