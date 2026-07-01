package com.languageschool.backend.dto.language;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateLanguageRequest {

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String name;
}
