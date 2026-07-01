package com.languageschool.backend.dto.exerciseOption;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class ExerciseOptionDto {
    Long id;
    Long exerciseId;
    String content;
    Integer orderNumber;
    Instant createdAt;
    Instant updatedAt;
}
