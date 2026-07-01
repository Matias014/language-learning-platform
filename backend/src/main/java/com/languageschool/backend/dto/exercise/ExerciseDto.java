package com.languageschool.backend.dto.exercise;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Value
@AllArgsConstructor
@Builder
public class ExerciseDto {
    Long id;
    Long lessonId;
    ExerciseType type;
    String question;
    Map<String, Object> answerSchema;
    String sampleAnswer;
    DifficultyLevel difficulty;
    Integer xp;
    Integer orderNumber;
    Long correctOptionId;
    BigDecimal passingScore;
    Instant createdAt;
    Instant updatedAt;
}
