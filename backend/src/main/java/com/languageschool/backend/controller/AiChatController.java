package com.languageschool.backend.controller;

import com.languageschool.backend.dto.ai.ChatDtos.ChatSendRequest;
import com.languageschool.backend.dto.chatMessage.ChatMessageDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.AiChatService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api")
public class AiChatController {

    private final ObjectProvider<AiChatService> aiChatServiceProvider;

    public AiChatController(ObjectProvider<AiChatService> aiChatServiceProvider) {
        this.aiChatServiceProvider = aiChatServiceProvider;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat-sessions/{sessionId}/ai-messages")
    public ResponseEntity<ChatMessageDto> sendAiMessage(@PathVariable Long sessionId,
                                                        @Valid @RequestBody ChatSendRequest request,
                                                        Authentication authentication) {
        AiChatService service = aiChatServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AI_CHAT_DISABLED);
        }
        String login = authentication.getName();
        ChatMessageDto dto = service.sendUserMessage(sessionId, login, request.getMessage(), request.getSystemPrompt());
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/chat-sessions/{sessionId}/messages/{id}")
                .buildAndExpand(sessionId, dto.getId())
                .toUri();
        return ResponseEntity.created(location).body(dto);
    }
}
