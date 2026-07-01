package com.languageschool.backend.service.impl;

import com.languageschool.backend.config.AiRuntimeLimits;
import com.languageschool.backend.dto.chatMessage.ChatMessageDto;
import com.languageschool.backend.dto.chatMessage.CreateChatMessageRequest;
import com.languageschool.backend.dto.chatMessage.UpdateChatMessageRequest;
import com.languageschool.backend.entity.ChatMessage;
import com.languageschool.backend.entity.ChatSession;
import com.languageschool.backend.entity.MessageSender;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.ChatMessageRepository;
import com.languageschool.backend.repository.ChatSessionRepository;
import com.languageschool.backend.service.ChatMessageService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository repo;
    private final ChatSessionRepository sessionRepo;
    private final AiRuntimeLimits limits;

    public ChatMessageServiceImpl(ChatMessageRepository repo,
                                  ChatSessionRepository sessionRepo,
                                  AiRuntimeLimits limits) {
        this.repo = repo;
        this.sessionRepo = sessionRepo;
        this.limits = limits;
    }

    @Override
    public List<ChatMessageDto> findAllAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);

        return repo.findAll().stream()
                .sorted(Comparator.comparing(ChatMessage::getSentAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ChatMessageDto create(Long sessionId, CreateChatMessageRequest req) {
        String safe = clampMessage(req.getMessage());
        ChatSession session = sessionRepo.findById(sessionId)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = session.getUser() != null ? session.getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        ChatMessage msg = ChatMessage.builder()
                .session(session)
                .sender(MessageSender.user)
                .message(safe)
                .build();
        return toDto(repo.save(msg));
    }

    @Override
    public ChatMessageDto getById(Long id) {
        ChatMessage m = repo.findById(id)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = m.getSession().getUser() != null ? m.getSession().getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        return toDto(m);
    }

    @Override
    public List<ChatMessageDto> findBySession(Long sessionId) {
        ChatSession s = sessionRepo.findById(sessionId)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = s.getUser() != null ? s.getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        return repo.findBySession_IdOrderBySentAtAsc(sessionId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ChatMessageDto update(Long id, UpdateChatMessageRequest req) {
        ChatMessage m = repo.findById(id)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = m.getSession().getUser() != null ? m.getSession().getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        boolean isAdmin = SecurityUtils.isAdmin(auth);
        if (!isAdmin) {
            if (m.getSender() != MessageSender.user) {
                throw ApiException.forbidden();
            }
        }

        if (req.getMessage() != null) {
            m.setMessage(clampMessage(req.getMessage()));
        }
        return toDto(repo.save(m));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        ChatMessage m = repo.findById(id)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = m.getSession().getUser() != null ? m.getSession().getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        boolean isAdmin = SecurityUtils.isAdmin(auth);
        if (!isAdmin) {
            if (m.getSender() != MessageSender.user) {
                throw ApiException.forbidden();
            }
        }

        repo.delete(m);
    }

    private String clampMessage(String s) {
        if (s == null) {
            throw ApiException.badRequest(ErrorCode.MESSAGE_REQUIRED);
        }
        String t = s.trim();
        if (t.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.MESSAGE_REQUIRED);
        }
        int max = limits.maxUserMessageChars();
        if (max > 0 && t.length() > max) {
            t = t.substring(0, max);
        }
        return t;
    }

    private ChatMessageDto toDto(ChatMessage m) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .sessionId(m.getSession().getId())
                .sender(m.getSender())
                .message(m.getMessage())
                .sentAt(m.getSentAt())
                .build();
    }
}
