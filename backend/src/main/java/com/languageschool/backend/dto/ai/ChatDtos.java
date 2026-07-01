package com.languageschool.backend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public final class ChatDtos {

    private ChatDtos() {
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChatSendRequest {
        @NotBlank
        @Size(max = 1200)
        private String message;

        @Size(max = 2000)
        private String systemPrompt;
    }
}
