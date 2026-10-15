package com.quickflow.plan;

import com.quickflow.common.SourceDeletedEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes the plan items of a deleted task, habit or learning card, and deletes plans that are left with no items
 * (D-5, BR10, BR14). Runs inside the transaction of the delete, so both happen or neither does.
 */
@Component
public class PlanItemCleanup {

    private final PlanItemRepository items;
    private final PlanRepository plans;

    public PlanItemCleanup(PlanItemRepository items, PlanRepository plans) {
        this.items = items;
        this.plans = plans;
    }

    @EventListener
    @Transactional
    public void onSourceDeleted(SourceDeletedEvent event) {
        List<PlanItemEntity> found = items.findBySourceTypeAndSourceId(event.type(), event.id());
        for (PlanItemEntity item : found) {
            PlanEntity plan = item.getPlan();
            plan.getItems().remove(item);
            if (plan.getItems().isEmpty()) {
                plans.delete(plan);
            }
        }
    }
}
