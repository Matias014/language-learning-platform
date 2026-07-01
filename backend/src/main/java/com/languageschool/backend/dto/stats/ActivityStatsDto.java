package com.languageschool.backend.dto.stats;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityStatsDto {
    private int streakDays;
    private int activeDaysCount;
    private Instant lastActivityAt;
}
