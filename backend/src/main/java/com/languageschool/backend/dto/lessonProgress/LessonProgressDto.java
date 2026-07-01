package com.languageschool.backend.dto.lessonProgress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LessonProgressDto {
    private Long lessonId;
    private Integer progressPercent;
}
