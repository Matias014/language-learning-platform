package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ExerciseOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseOptionRepository extends JpaRepository<ExerciseOption, Long> {

    List<ExerciseOption> findByExercise_IdOrderByOrderNumberAsc(Long exerciseId);

    boolean existsByExercise_IdAndOrderNumberAndDeletedAtIsNull(Long exerciseId, Integer orderNumber);
}
