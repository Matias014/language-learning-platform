package com.languageschool.backend.service;

import com.languageschool.backend.dto.exerciseAward.CreateExerciseAwardRequest;
import com.languageschool.backend.dto.exerciseAward.ExerciseAwardDto;

import java.util.List;
import java.util.Optional;

public interface ExerciseAwardService {
    ExerciseAwardDto create(CreateExerciseAwardRequest req);

    Optional<ExerciseAwardDto> createIfEligible(Long attemptId);

    Optional<ExerciseAwardDto> findById(Long id);

    Optional<ExerciseAwardDto> findByUserAndExercise(Long userId, Long exerciseId);

    Optional<ExerciseAwardDto> findByAttempt(Long attemptId);

    List<ExerciseAwardDto> findByUser(Long userId);

    void delete(Long id);

    List<ExerciseAwardDto> findAll();
}
