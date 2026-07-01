package com.languageschool.backend.dto.exercise;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateExerciseRequest {

    @NotNull
    private Long lessonId;

    @NotNull
    private ExerciseType type;

    @NotBlank
    @Size(max = 10000)
    private String question;

    private Map<String, Object> answerSchema;

    @Size(max = 10000)
    private String sampleAnswer;

    @NotNull
    private DifficultyLevel difficulty;

    @NotNull
    @Min(0)
    private Integer xp;

    @NotNull
    @Min(1)
    private Integer orderNumber;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal passingScore;
}
