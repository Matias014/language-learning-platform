package com.languageschool.backend.controller;

import com.languageschool.backend.dto.chatMessage.ChatMessageDto;
import com.languageschool.backend.dto.chatMessage.CreateChatMessageRequest;
import com.languageschool.backend.dto.chatMessage.UpdateChatMessageRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.ChatMessageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class ChatMessageController {

    private final ChatMessageService service;

    public ChatMessageController(ChatMessageService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chat-messages")
    public List<ChatMessageDto> listAll() {
        return service.findAllAdmin();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat-sessions/{sessionId}/messages")
    public ResponseEntity<ChatMessageDto> create(@PathVariable Long sessionId, @Valid @RequestBody CreateChatMessageRequest req) {
        ChatMessageDto dto = service.create(sessionId, req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chat-messages/{id}")
    public ChatMessageDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chat-sessions/{sessionId}/messages")
    public List<ChatMessageDto> listBySession(@PathVariable Long sessionId) {
        return service.findBySession(sessionId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chat-sessions/{sessionId}/messages/{id}")
    public ChatMessageDto getInSession(@PathVariable Long sessionId, @PathVariable Long id) {
        ChatMessageDto dto = service.getById(id);
        if (!Objects.equals(dto.getSessionId(), sessionId)) {
            throw ApiException.notFound();
        }
        return dto;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/chat-messages/{id}")
    public ChatMessageDto update(@PathVariable Long id, @Valid @RequestBody UpdateChatMessageRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/chat-messages/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/chat-sessions/{sessionId}/messages/{id}")
    public ChatMessageDto updateInSession(@PathVariable Long sessionId, @PathVariable Long id, @Valid @RequestBody UpdateChatMessageRequest req) {
        ChatMessageDto existing = service.getById(id);
        if (!Objects.equals(existing.getSessionId(), sessionId)) {
            throw ApiException.notFound();
        }
        return service.update(id, req);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/chat-sessions/{sessionId}/messages/{id}")
    public ResponseEntity<Void> deleteInSession(@PathVariable Long sessionId, @PathVariable Long id) {
        ChatMessageDto existing = service.getById(id);
        if (!Objects.equals(existing.getSessionId(), sessionId)) {
            throw ApiException.notFound();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
