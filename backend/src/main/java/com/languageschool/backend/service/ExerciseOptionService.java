package com.languageschool.backend.service;

import com.languageschool.backend.dto.exerciseOption.CreateExerciseOptionRequest;
import com.languageschool.backend.dto.exerciseOption.ExerciseOptionDto;
import com.languageschool.backend.dto.exerciseOption.UpdateExerciseOptionRequest;

import java.util.List;
import java.util.Optional;

public interface ExerciseOptionService {

    ExerciseOptionDto create(CreateExerciseOptionRequest req);

    Optional<ExerciseOptionDto> findById(Long id);

    List<ExerciseOptionDto> findByExercise(Long exerciseId);

    ExerciseOptionDto update(Long id, UpdateExerciseOptionRequest req);

    void delete(Long id);
}
