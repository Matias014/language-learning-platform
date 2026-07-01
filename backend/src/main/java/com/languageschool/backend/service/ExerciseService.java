package com.languageschool.backend.service;

import com.languageschool.backend.dto.exercise.CreateExerciseRequest;
import com.languageschool.backend.dto.exercise.ExerciseDto;
import com.languageschool.backend.dto.exercise.UpdateExerciseRequest;

import java.util.List;
import java.util.Optional;

public interface ExerciseService {

    ExerciseDto create(CreateExerciseRequest req);

    Optional<ExerciseDto> findById(Long id);

    List<ExerciseDto> findByLesson(Long lessonId);

    ExerciseDto update(Long id, UpdateExerciseRequest req);

    void delete(Long id);
}
