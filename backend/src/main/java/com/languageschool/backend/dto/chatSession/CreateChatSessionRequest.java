package com.languageschool.backend.dto.chatSession;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChatSessionRequest {

    @Size(min = 2, max = 10)
    @Pattern(regexp = "^[a-z]{2,3}(-[A-Z]{2})?$")
    private String conversationLanguageCode;

    @Size(max = 200)
    private String title;

    @Size(max = 2000)
    private String systemPrompt;
}
