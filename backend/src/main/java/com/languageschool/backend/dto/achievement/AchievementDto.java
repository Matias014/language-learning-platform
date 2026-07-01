package com.languageschool.backend.dto.achievement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class AchievementDto {
    Long id;
    String title;
    String description;
    String iconPath;
    Integer requiredXp;
}
