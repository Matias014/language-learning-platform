package com.languageschool.backend.dto.exerciseOption;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExerciseOptionRequest {

    @NotNull
    private Long exerciseId;

    @NotBlank
    @Size(max = 10000)
    private String content;

    @NotNull
    @Min(1)
    private Integer orderNumber;
}
