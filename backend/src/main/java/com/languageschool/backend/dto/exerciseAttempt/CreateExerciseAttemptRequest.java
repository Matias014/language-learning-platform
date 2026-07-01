package com.languageschool.backend.dto.exerciseAttempt;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExerciseAttemptRequest {

//   userid z jwt brac, notnull

    @NotNull
    private Long exerciseId;

    @Size(max = 10000)
    private String submittedAnswer;

    private Long chosenOptionId;

    @Min(0)
    private Integer durationSeconds;
}
