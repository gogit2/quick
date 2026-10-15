package com.quickflow.habit;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HabitCompletionRepository extends JpaRepository<HabitCompletionEntity, Long> {

    List<HabitCompletionEntity> findByHabitIdOrderByCompletionDateDesc(long habitId);

    Optional<HabitCompletionEntity> findByHabitIdAndCompletionDate(long habitId, LocalDate completionDate);

    boolean existsByHabitIdAndCompletionDate(long habitId, LocalDate completionDate);

    void deleteByHabitId(long habitId);
}
