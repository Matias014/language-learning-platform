package com.languageschool.backend.service;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;

import java.util.List;

public interface AiExerciseService {
    List<Long> generate(Long lessonId,
                        ExerciseType type,
                        DifficultyLevel difficulty,
                        String topic,
                        Integer count,
                        Integer xp);
}
