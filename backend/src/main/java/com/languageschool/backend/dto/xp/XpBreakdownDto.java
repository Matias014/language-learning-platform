package com.languageschool.backend.dto.xp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class XpBreakdownDto {
    private int days;
    private Map<String, Integer> byType;
    private Map<String, Integer> byDifficulty;
}
