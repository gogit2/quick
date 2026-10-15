package com.quickflow.plan;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/** Entity to response mapping. Plain class: status, rest time and progress are computed from the entity and now. */
public final class PlanMapper {

    private PlanMapper() {
    }

    /** titleOf resolves the current title of an item's source (task title, habit name, card title). */
    public static Plan toDto(PlanEntity e, Instant now, Function<PlanItemEntity, String> titleOf) {
        List<PlanItem> items = e.getItems().stream()
                .map(i -> new PlanItem(i.getId(), i.getSourceType(), i.getSourceId(), titleOf.apply(i), i.isDone()))
                .toList();
        int totalItems = items.size();
        int doneItems = (int) items.stream().filter(PlanItem::done).count();
        PlanStatus status = PlanStatusCalculator.statusOf(
                e.getStartDateTime(), e.getEndDateTime(), doneItems, totalItems, now);
        return new Plan(
                e.getId(),
                e.getTitle(),
                e.getEstimatedDurationMinutes(),
                e.getStartDateTime(),
                e.getEndDateTime(),
                e.getPriorityOrder(),
                status,
                e.getCreatedAt(),
                e.isStartNotified(),
                isDurationLongerThanWindow(e),
                totalItems,
                doneItems,
                PlanStatusCalculator.completionPercent(doneItems, totalItems),
                PlanStatusCalculator.restSeconds(status, e.getEndDateTime(), now),
                items);
    }

    /** Warning only (D-9): the estimate is longer than the window between start and end. */
    public static boolean isDurationLongerThanWindow(PlanEntity e) {
        Duration window = Duration.between(e.getStartDateTime(), e.getEndDateTime());
        return window.compareTo(Duration.ofMinutes(e.getEstimatedDurationMinutes())) < 0;
    }
}
