package com.languageschool.backend.dto.courseEnrollment;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseEnrollmentRequest {

    @NotNull
    private Long courseId;
}
