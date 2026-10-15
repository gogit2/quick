package com.quickflow.learning;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A note row (table learning_note) of a learning card. */
@Entity
@Table(name = "learning_note")
public class LearningNoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "learning_card_id", nullable = false)
    private LearningCardEntity card;

    private String text;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;

    protected LearningNoteEntity() {
    }

    public LearningNoteEntity(String text, Instant now) {
        this.text = text;
        this.createdAt = now;
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

    public String getText() {
        return text;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
