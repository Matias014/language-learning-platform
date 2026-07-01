package com.languageschool.backend.dto.adminStats;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HardestExerciseDto {
    private Long exerciseId;
    private BigDecimal averageAccuracy;
    private Integer averageDurationSeconds;
}
