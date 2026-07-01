package com.languageschool.backend.dto.userAchievement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class UserAchievementDto {
    Long id;
    Long userId;
    Long achievementId;
    Instant earnedAt;
}
