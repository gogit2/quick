package com.quickflow.habit;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    List<HabitEntity> findByActive(boolean active, Sort sort);
}
