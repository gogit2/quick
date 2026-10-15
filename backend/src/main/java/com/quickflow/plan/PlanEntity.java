package com.quickflow.plan;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A plan row (table plan). Status is never stored: PlanService computes it on every read. */
@Entity
@Table(name = "plan")
public class PlanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private int estimatedDurationMinutes;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant startDateTime;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant endDateTime;

    private int priorityOrder;

    @JdbcTypeCode(SqlTypes.TIMESTAMP)
    private Instant createdAt;

    private boolean startNotified;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<PlanItemEntity> items = new ArrayList<>();

    protected PlanEntity() {
    }

    public PlanEntity(String title, int estimatedDurationMinutes, Instant startDateTime, Instant endDateTime,
                      int priorityOrder, Instant createdAt) {
        this.title = title;
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.priorityOrder = priorityOrder;
        this.createdAt = createdAt;
        this.startNotified = false;
    }

    /** Adds an item and links it to this plan. */
    public void addItem(PlanItemEntity item) {
        items.add(item);
        item.setPlan(this);
    }

    public Optional<PlanItemEntity> findItem(long itemId) {
        return items.stream().filter(i -> i.getId() != null && i.getId() == itemId).findFirst();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public Instant getStartDateTime() {
        return startDateTime;
    }

    public Instant getEndDateTime() {
        return endDateTime;
    }

    public int getPriorityOrder() {
        return priorityOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isStartNotified() {
        return startNotified;
    }

    public void setStartNotified(boolean startNotified) {
        this.startNotified = startNotified;
    }

    public List<PlanItemEntity> getItems() {
        return items;
    }
}
