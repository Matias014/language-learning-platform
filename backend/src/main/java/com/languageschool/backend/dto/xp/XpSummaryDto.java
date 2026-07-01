package com.languageschool.backend.dto.xp;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class XpSummaryDto {
    private long totalXp;
    private long awardsCount;
}
