package com.languageschool.backend.dto.chatMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateChatMessageRequest {
    @NotBlank
    @Size(max = 1200)
    private String message;
}
