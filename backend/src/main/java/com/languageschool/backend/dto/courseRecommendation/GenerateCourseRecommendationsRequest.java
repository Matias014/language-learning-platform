package com.languageschool.backend.dto.courseRecommendation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateCourseRecommendationsRequest {

    @NotNull
    @Min(1)
    @Max(20)
    private Integer limit;

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String learningLanguageCode;

    @NotBlank
    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String fromLanguageCode;

    @NotBlank
    @Pattern(regexp = "^(A1|A2|B1|B2|C1|C2)$")
    private String levelCode;
}
