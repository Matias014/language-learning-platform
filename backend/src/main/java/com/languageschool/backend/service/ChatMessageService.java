package com.languageschool.backend.service;

import com.languageschool.backend.dto.chatMessage.ChatMessageDto;
import com.languageschool.backend.dto.chatMessage.CreateChatMessageRequest;
import com.languageschool.backend.dto.chatMessage.UpdateChatMessageRequest;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessageDto> findAllAdmin();

    ChatMessageDto create(Long sessionId, CreateChatMessageRequest req);

    ChatMessageDto getById(Long id);

    List<ChatMessageDto> findBySession(Long sessionId);

    ChatMessageDto update(Long id, UpdateChatMessageRequest req);

    void delete(Long id);
}
