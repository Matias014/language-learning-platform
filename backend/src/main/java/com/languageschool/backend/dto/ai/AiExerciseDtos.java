package com.languageschool.backend.dto.ai;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AiExerciseDtos {

    public record GenerateExercisesRequest(
            @NotNull ExerciseType exerciseType,
            @NotNull DifficultyLevel difficultyLevel,
            @NotBlank @Size(min = 1, max = 100) String topic,
            @NotNull @Min(1) Integer count,
            @Min(0) Integer xp
    ) {
    }
}
