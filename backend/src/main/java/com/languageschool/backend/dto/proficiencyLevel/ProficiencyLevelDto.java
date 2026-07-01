package com.languageschool.backend.dto.proficiencyLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class ProficiencyLevelDto {
    String code;
    String name;
    Integer orderNumber;
}
