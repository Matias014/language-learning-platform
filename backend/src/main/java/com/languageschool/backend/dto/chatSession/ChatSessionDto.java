package com.languageschool.backend.dto.chatSession;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class ChatSessionDto {
    Long id;
    Long userId;
    String conversationLanguageCode;
    String title;
    String systemPrompt;
    Instant startedAt;
}
