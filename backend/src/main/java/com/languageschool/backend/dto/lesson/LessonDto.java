package com.languageschool.backend.dto.lesson;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class LessonDto {
    Long id;
    Long courseId;
    String title;
    String description;
    Integer orderNumber;
    Instant createdAt;
    Instant updatedAt;
}
