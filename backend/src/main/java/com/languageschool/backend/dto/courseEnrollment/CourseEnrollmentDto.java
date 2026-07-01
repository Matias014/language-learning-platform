package com.languageschool.backend.dto.courseEnrollment;

import com.languageschool.backend.entity.CourseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class CourseEnrollmentDto {
    Long id;
    Long userId;
    Long courseId;
    Long currentLessonId;
    CourseStatus status;
    Instant startedAt;
    Instant completedAt;
    Instant lastActivityAt;
}
