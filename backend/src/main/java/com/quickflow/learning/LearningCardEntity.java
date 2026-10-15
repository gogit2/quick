package com.quickflow.learning;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A learning card row (table learning_card). Milestones and notes belong to the card and go with it. */
@Entity
@Table(name = "learning_card")
public class LearningCardEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private LearningStatus status;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LearningMilestoneEntity> milestones = new ArrayList<>();

    @OneToMany(mappedBy = "card", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LearningNoteEntity> notes = new ArrayList<>();

    protected LearningCardEntity() {
    }

    public LearningCardEntity(String title, String description, LearningStatus status, Instant now) {
        this.title = title;
        this.description = description;
        this.status = status;
        this.createdAt = now;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LearningStatus getStatus() {
        return status;
    }

    public void setStatus(LearningStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LearningMilestoneEntity> getMilestones() {
        return milestones;
    }

    public List<LearningNoteEntity> getNotes() {
        return notes;
    }

    public void addMilestone(LearningMilestoneEntity milestone) {
        milestone.setCard(this);
        milestones.add(milestone);
    }

    public void addNote(LearningNoteEntity note) {
        note.setCard(this);
        notes.add(note);
    }
}
