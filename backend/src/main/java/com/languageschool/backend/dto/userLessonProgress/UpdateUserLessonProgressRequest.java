package com.languageschool.backend.dto.userLessonProgress;

import com.languageschool.backend.entity.LessonStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateUserLessonProgressRequest {

    private LessonStatus status;
}
