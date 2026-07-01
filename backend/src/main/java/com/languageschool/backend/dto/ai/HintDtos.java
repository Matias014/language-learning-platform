package com.languageschool.backend.dto.ai;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public final class HintDtos {

    private HintDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HintRequest {
        @NotNull
        private Long exerciseId;

        @Size(max = 2000)
        private String userAnswer;

        @Min(1)
        @Max(5)
        @Builder.Default
        @JsonSetter(nulls = Nulls.SKIP)
        private Integer maxHints = 3;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class HintResponse {
        private boolean correct;
        private String feedback;
        @Builder.Default
        private List<String> hints = List.of();
    }
}
