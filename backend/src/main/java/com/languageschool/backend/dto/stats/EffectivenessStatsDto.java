package com.languageschool.backend.dto.stats;

import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;

import java.math.BigDecimal;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectivenessStatsDto {
    private BigDecimal globalAccuracy;
    private Map<ExerciseType, BigDecimal> accuracyByType;
    private Map<DifficultyLevel, BigDecimal> accuracyByDifficulty;
    private BigDecimal averageOpenScore;
}
