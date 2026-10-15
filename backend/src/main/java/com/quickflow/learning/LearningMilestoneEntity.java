package com.quickflow.learning;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A milestone row (table learning_milestone). Business rules live in LearningService. */
@Entity
@Table(name = "learning_milestone")
public class LearningMilestoneEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "learning_card_id", nullable = false)
    private LearningCardEntity card;

    private String title;

    private boolean done;

    private LocalDate targetDate;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant completedAt;

    protected LearningMilestoneEntity() {
    }

    public LearningMilestoneEntity(String title, LocalDate targetDate) {
        this.title = title;
        this.targetDate = targetDate;
    }

    public Long getId() {
        return id;
    }

    public LearningCardEntity getCard() {
        return card;
    }

    void setCard(LearningCardEntity card) {
        this.card = card;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
