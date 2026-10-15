package com.quickflow.plan;

import com.quickflow.common.PlanItemSourceType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * One item of a plan (table plan_item). The source is polymorphic (task, habit or learning card), so there is
 * no foreign key to it; PlanItemCleanup removes items when a source is deleted (D-5).
 */
@Entity
@Table(name = "plan_item")
public class PlanItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "plan_id", nullable = false)
    private PlanEntity plan;

    @Enumerated(EnumType.STRING)
    private PlanItemSourceType sourceType;

    private long sourceId;

    private boolean done;

    protected PlanItemEntity() {
    }

    public PlanItemEntity(PlanItemSourceType sourceType, long sourceId) {
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.done = false;
    }

    public Long getId() {
        return id;
    }

    public PlanEntity getPlan() {
        return plan;
    }

    void setPlan(PlanEntity plan) {
        this.plan = plan;
    }

    public PlanItemSourceType getSourceType() {
        return sourceType;
    }

    public long getSourceId() {
        return sourceId;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }
}
