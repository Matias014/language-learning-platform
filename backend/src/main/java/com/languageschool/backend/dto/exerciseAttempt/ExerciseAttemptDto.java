package com.languageschool.backend.dto.exerciseAttempt;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class ExerciseAttemptDto {
    Long id;
    Long userId;
    Long exerciseId;
    String submittedAnswer;
    Long chosenOptionId;
    boolean correct;
    BigDecimal score;
    String feedback;
    Integer attemptNumber;
    Instant submittedAt;
    Integer durationSeconds;
}
