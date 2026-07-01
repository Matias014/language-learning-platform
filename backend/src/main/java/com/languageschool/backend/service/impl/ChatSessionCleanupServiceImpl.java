package com.languageschool.backend.service.impl;

import com.languageschool.backend.entity.ChatMessage;
import com.languageschool.backend.entity.ChatSession;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.ChatMessageRepository;
import com.languageschool.backend.repository.ChatSessionRepository;
import com.languageschool.backend.repository.LlmLogRepository;
import com.languageschool.backend.service.ChatSessionCleanupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatSessionCleanupServiceImpl implements ChatSessionCleanupService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final LlmLogRepository llmLogRepository;

    public ChatSessionCleanupServiceImpl(ChatSessionRepository chatSessionRepository,
                                         ChatMessageRepository chatMessageRepository,
                                         LlmLogRepository llmLogRepository) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.llmLogRepository = llmLogRepository;
    }

    @Override
    @Transactional
    public void deleteSessionCascade(Long sessionId, String requesterLogin, boolean requesterIsAdmin) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(ApiException::notFound);

        if (!requesterIsAdmin) {
            var owner = session.getUser();
            String ownerLogin = owner != null ? owner.getLogin() : null;
            if (ownerLogin == null || requesterLogin == null || !ownerLogin.equalsIgnoreCase(requesterLogin)) {
                throw ApiException.forbidden();
            }
        }

        llmLogRepository.deleteByChatSession_Id(sessionId);

        List<ChatMessage> msgs = chatMessageRepository.findBySession_IdOrderBySentAtAsc(sessionId);
        if (!msgs.isEmpty()) {
            chatMessageRepository.deleteAll(msgs);
        }

        chatSessionRepository.delete(session);
    }
}
