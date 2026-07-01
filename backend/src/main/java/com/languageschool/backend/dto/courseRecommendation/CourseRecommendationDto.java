package com.languageschool.backend.dto.courseRecommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class CourseRecommendationDto {
    Long id;
    Long userId;
    Long courseId;
    BigDecimal score;
    Instant createdAt;
}
