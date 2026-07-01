package com.languageschool.backend.dto.lesson;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLessonRequest {

    @Size(max = 200)
    private String title;

    @Size(max = 10000)
    private String description;

    @Min(1)
    private Integer orderNumber;
}
