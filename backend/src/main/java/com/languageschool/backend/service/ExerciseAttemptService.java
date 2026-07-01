package com.languageschool.backend.service;

import com.languageschool.backend.dto.exerciseAttempt.CreateExerciseAttemptRequest;
import com.languageschool.backend.dto.exerciseAttempt.ExerciseAttemptDto;

import java.util.List;
import java.util.Optional;

public interface ExerciseAttemptService {

    ExerciseAttemptDto create(Long userId, CreateExerciseAttemptRequest req);

    ExerciseAttemptDto getById(Long id);

    List<ExerciseAttemptDto> findByUserAndExercise(Long userId, Long exerciseId);

    List<ExerciseAttemptDto> findByUser(Long userId);

    List<ExerciseAttemptDto> findByExercise(Long exerciseId);

    Optional<ExerciseAttemptDto> findLatestByUserAndExercise(Long userId, Long exerciseId);

    void delete(Long id);

    List<ExerciseAttemptDto> findAll();
}
