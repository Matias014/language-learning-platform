package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ExerciseAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ExerciseAttemptRepository extends JpaRepository<ExerciseAttempt, Long> {

    List<ExerciseAttempt> findByUser_IdAndExercise_IdOrderBySubmittedAtDesc(Long userId, Long exerciseId);

    List<ExerciseAttempt> findByUser_IdOrderBySubmittedAtDesc(Long userId);

    List<ExerciseAttempt> findByExercise_IdOrderBySubmittedAtDesc(Long exerciseId);

    Optional<ExerciseAttempt> findTopByUser_IdAndExercise_IdOrderByAttemptNumberDesc(Long userId, Long exerciseId);
}
