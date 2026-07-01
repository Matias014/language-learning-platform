package com.languageschool.backend.dto.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class CourseDto {
    Long id;
    String learningLanguageCode;
    String fromLanguageCode;
    String title;
    String description;
    String levelCode;
    String countryIconPath;
    Instant createdAt;
    Instant updatedAt;
}
