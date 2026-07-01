package com.languageschool.backend.service.impl;

import com.languageschool.backend.config.AiRuntimeLimits;
import com.languageschool.backend.dto.chatSession.ChatSessionDto;
import com.languageschool.backend.dto.chatSession.CreateChatSessionRequest;
import com.languageschool.backend.dto.chatSession.UpdateChatSessionRequest;
import com.languageschool.backend.entity.ChatSession;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.ChatSessionRepository;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.ChatSessionCleanupService;
import com.languageschool.backend.service.ChatSessionService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionRepository chatSessionRepository;
    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final ChatSessionCleanupService cleanupService;
    private final AiRuntimeLimits aiLimits;

    public ChatSessionServiceImpl(ChatSessionRepository chatSessionRepository,
                                  UserRepository userRepository,
                                  LanguageRepository languageRepository,
                                  ChatSessionCleanupService cleanupService,
                                  AiRuntimeLimits aiLimits) {
        this.chatSessionRepository = chatSessionRepository;
        this.userRepository = userRepository;
        this.languageRepository = languageRepository;
        this.cleanupService = cleanupService;
        this.aiLimits = aiLimits;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private String clampSystemPrompt(String s) {
        String t = trim(s);
        if (t == null || t.isEmpty()) {
            return null;
        }
        int max = aiLimits.maxSystemPromptChars();
        if (max > 0 && t.length() > max) {
            return t.substring(0, max);
        }
        return t;
    }

    @Transactional
    @Override
    public ChatSessionDto create(CreateChatSessionRequest dto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }

        User me = userRepository.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);

        ChatSession s = new ChatSession();
        s.setUser(me);

        String langCode = trim(dto.getConversationLanguageCode());
        if (langCode != null && !langCode.isEmpty()) {
            Language lang = languageRepository.findById(langCode)
                    .orElseThrow(ApiException::notFound);
            s.setConversationLanguage(lang);
        }

        s.setTitle(trim(dto.getTitle()));
        s.setSystemPrompt(clampSystemPrompt(dto.getSystemPrompt()));

        return toDto(chatSessionRepository.save(s));
    }

    @Override
    public ChatSessionDto getById(Long id) {
        ChatSession s = chatSessionRepository.findById(id)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = s.getUser() != null ? s.getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        return toDto(s);
    }

    @Override
    public List<ChatSessionDto> listMy() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        User me = userRepository.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);
        return chatSessionRepository.findByUser_IdOrderByStartedAtDesc(me.getId())
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<ChatSessionDto> findByUser(Long userId) {
        String owner = userRepository.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, owner);
        return chatSessionRepository.findByUser_IdOrderByStartedAtDesc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<ChatSessionDto> findAllAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        return chatSessionRepository
                .findAll(Sort.by(Sort.Direction.DESC, "startedAt"))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public ChatSessionDto update(Long id, UpdateChatSessionRequest dto) {
        ChatSession s = chatSessionRepository.findById(id)
                .orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String ownerLogin = s.getUser() != null ? s.getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        if (dto.getConversationLanguageCode() != null) {
            String langCode = trim(dto.getConversationLanguageCode());
            if (langCode == null || langCode.isEmpty()) {
                s.setConversationLanguage(null);
            } else {
                Language lang = languageRepository.findById(langCode)
                        .orElseThrow(ApiException::notFound);
                s.setConversationLanguage(lang);
            }
        }
        if (dto.getTitle() != null) {
            s.setTitle(trim(dto.getTitle()));
        }
        if (dto.getSystemPrompt() != null) {
            s.setSystemPrompt(clampSystemPrompt(dto.getSystemPrompt()));
        }

        return toDto(chatSessionRepository.save(s));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }

        ChatSession s = chatSessionRepository.findById(id)
                .orElseThrow(ApiException::notFound);

        String ownerLogin = s.getUser() != null ? s.getUser().getLogin() : null;
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);

        boolean isAdmin = SecurityUtils.isAdmin(auth);
        cleanupService.deleteSessionCascade(id, auth.getName(), isAdmin);
    }

    private ChatSessionDto toDto(ChatSession s) {
        return ChatSessionDto.builder()
                .id(s.getId())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .conversationLanguageCode(
                        s.getConversationLanguage() != null
                                ? s.getConversationLanguage().getCode()
                                : null)
                .title(s.getTitle())
                .systemPrompt(s.getSystemPrompt())
                .startedAt(s.getStartedAt())
                .build();
    }
}
