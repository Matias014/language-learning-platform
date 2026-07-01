package com.languageschool.backend.dto.language;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

@Value
@AllArgsConstructor
@Builder
public class LanguageDto {
    String code;
    String name;
}
