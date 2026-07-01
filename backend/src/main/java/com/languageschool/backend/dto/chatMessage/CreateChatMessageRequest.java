package com.languageschool.backend.dto.chatMessage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatMessageRequest {

    @NotBlank
    @Size(max = 1200)
    private String message;
}
