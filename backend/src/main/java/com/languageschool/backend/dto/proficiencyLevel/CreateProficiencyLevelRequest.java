package com.languageschool.backend.dto.proficiencyLevel;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateProficiencyLevelRequest {

    @NotBlank
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$")
    private String code;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotNull
    @Min(1)
    private Integer orderNumber;
}
