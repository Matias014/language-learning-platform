package com.languageschool.backend.dto.achievement;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateAchievementRequest {
    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String description;

    @Size(max = 2048)
    private String iconPath;

    @Min(0)
    private Integer requiredXp;
}
