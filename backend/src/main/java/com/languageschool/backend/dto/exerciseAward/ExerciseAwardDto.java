package com.languageschool.backend.dto.exerciseAward;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class ExerciseAwardDto {
    Long id;
    Long attemptId;
    Integer awardedXp;
    Instant awardedAt;
}
