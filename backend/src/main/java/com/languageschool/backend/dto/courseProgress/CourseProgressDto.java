package com.languageschool.backend.dto.courseProgress;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressDto {
    private Long courseId;
    private Integer progressPercent;
}
