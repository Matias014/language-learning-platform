package com.languageschool.backend.dto.userAchievement;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserAchievementRequest {

    // userid z jwt

    @NotNull
    private Long achievementId;
}
