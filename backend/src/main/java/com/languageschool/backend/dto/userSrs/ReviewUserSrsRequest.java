package com.languageschool.backend.dto.userSrs;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUserSrsRequest {

    @NotNull
    private Long exerciseId;

    @Min(0)
    @Max(5)
    private Integer quality;
}
