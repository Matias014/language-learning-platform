package com.languageschool.backend.dto.userLevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLevelRequest {

    @NotNull
    @Min(1)
    private Integer level;

    @NotNull
    @Min(0)
    private Integer requiredXp;
}
