package com.languageschool.backend.service;

import com.languageschool.backend.dto.chatSession.ChatSessionDto;
import com.languageschool.backend.dto.chatSession.CreateChatSessionRequest;
import com.languageschool.backend.dto.chatSession.UpdateChatSessionRequest;

import java.util.List;

public interface ChatSessionService {
    ChatSessionDto create(CreateChatSessionRequest dto);

    ChatSessionDto getById(Long id);

    List<ChatSessionDto> listMy();

    List<ChatSessionDto> findByUser(Long userId);

    List<ChatSessionDto> findAllAdmin();

    ChatSessionDto update(Long id, UpdateChatSessionRequest dto);

    void delete(Long id);
}
