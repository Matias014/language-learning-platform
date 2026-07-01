package com.languageschool.backend.dto.language;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateLanguageRequest {

    @Size(max = 100)
    private String name;
}
