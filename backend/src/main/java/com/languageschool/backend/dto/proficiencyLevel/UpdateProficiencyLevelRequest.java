package com.languageschool.backend.dto.proficiencyLevel;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProficiencyLevelRequest {

    @Size(max = 50)
    private String name;

    @Min(1)
    private Integer orderNumber;
}
