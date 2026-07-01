package com.languageschool.backend.dto.adminStats;

import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmStatus;

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
public class LlmStatsDto {
    private long calls;
    private long tokensIn;
    private long tokensOut;
    private BigDecimal averageLatencyMs;
    private Map<InteractionType, Long> callsByInteractionType;
    private Map<LlmStatus, Long> callsByStatus;
}
