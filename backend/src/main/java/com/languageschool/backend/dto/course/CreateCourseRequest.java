package com.languageschool.backend.dto.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateCourseRequest {

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String learningLanguageCode;

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String fromLanguageCode;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 10000)
    private String description;

    @NotBlank
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$")
    private String levelCode;

    @Size(max = 2048)
    private String countryIconPath;
}
