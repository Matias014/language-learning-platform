package com.languageschool.backend.dto.exercise;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateExerciseRequest {

    private Long lessonId;

    private ExerciseType type;

    @Size(max = 10000)
    private String question;

    private Map<String, Object> answerSchema;

    @Size(max = 10000)
    private String sampleAnswer;

    private DifficultyLevel difficulty;

    @Min(0)
    private Integer xp;

    @Min(1)
    private Integer orderNumber;

    private Long correctOptionId;

    @DecimalMin("0.00")
    @DecimalMax("100.00")
    private BigDecimal passingScore;
}
