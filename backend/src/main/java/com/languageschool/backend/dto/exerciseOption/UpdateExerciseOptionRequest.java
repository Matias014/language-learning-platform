package com.languageschool.backend.dto.exerciseOption;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExerciseOptionRequest {

    @Size(max = 10000)
    private String content;

    @Min(1)
    private Integer orderNumber;
}
