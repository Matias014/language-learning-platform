package com.languageschool.backend.dto.courseEnrollment;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCourseEnrollmentRequest {

    private Long currentLessonId;
}
