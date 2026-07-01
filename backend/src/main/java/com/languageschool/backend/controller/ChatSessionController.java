package com.languageschool.backend.controller;

import com.languageschool.backend.dto.chatSession.ChatSessionDto;
import com.languageschool.backend.dto.chatSession.CreateChatSessionRequest;
import com.languageschool.backend.dto.chatSession.UpdateChatSessionRequest;
import com.languageschool.backend.service.ChatSessionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class ChatSessionController {

    private final ChatSessionService service;

    public ChatSessionController(ChatSessionService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chat-sessions")
    public List<ChatSessionDto> listAll() {
        return service.findAllAdmin();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/chat-sessions")
    public ResponseEntity<ChatSessionDto> create(@Valid @RequestBody CreateChatSessionRequest req) {
        ChatSessionDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/chat-sessions/{id}")
    public ChatSessionDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/chat-sessions")
    public List<ChatSessionDto> listMy() {
        return service.listMy();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/chat-sessions")
    public List<ChatSessionDto> listByUser(@PathVariable Long userId) {
        return service.findByUser(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/chat-sessions/{id}")
    public ChatSessionDto update(@PathVariable Long id, @Valid @RequestBody UpdateChatSessionRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/chat-sessions/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
