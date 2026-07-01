package com.languageschool.backend.dto.chatMessage;

import com.languageschool.backend.entity.MessageSender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@AllArgsConstructor
@Builder
public class ChatMessageDto {
    Long id;
    Long sessionId;
    MessageSender sender;
    String message;
    Instant sentAt;
}
