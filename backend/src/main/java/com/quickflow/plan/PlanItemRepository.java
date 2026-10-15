package com.quickflow.plan;

import com.quickflow.common.PlanItemSourceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanItemRepository extends JpaRepository<PlanItemEntity, Long> {

    List<PlanItemEntity> findBySourceTypeAndSourceId(PlanItemSourceType sourceType, long sourceId);
}
