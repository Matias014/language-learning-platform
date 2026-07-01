package com.languageschool.backend.dto.userLessonProgress;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserLessonProgressRequest {

//    userid brac z jwt

    @NotNull
    private Long lessonId;
}
