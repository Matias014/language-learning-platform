package com.languageschool.backend.dto.userLessonProgress;

import com.languageschool.backend.entity.LessonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class UserLessonProgressDto {
    Long id;
    Long userId;
    Long lessonId;
    LessonStatus status;
    Instant completedAt;
    Instant lastActivityAt;
}
