package com.languageschool.backend.dto.userLevel;

import jakarta.validation.constraints.Min;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLevelRequest {

    @Min(0)
    private Integer requiredXp;
}
