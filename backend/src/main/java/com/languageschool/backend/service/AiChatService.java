package com.languageschool.backend.service;

import com.languageschool.backend.dto.chatMessage.ChatMessageDto;

public interface AiChatService {
    ChatMessageDto sendUserMessage(Long sessionId, String login, String text, String systemPromptOverride);
}
