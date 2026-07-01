package com.languageschool.backend.dto.userLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class LevelDto {
    Integer level;
    Integer requiredXp;
}
