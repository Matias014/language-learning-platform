package com.languageschool.backend.controller;

import com.languageschool.backend.dto.llmLog.LlmLogDto;
import com.languageschool.backend.service.LlmLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LlmLogController {

    private final LlmLogService service;

    public LlmLogController(LlmLogService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/llm-logs")
    public List<LlmLogDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/llm-logs")
    public List<LlmLogDto> byUser(@PathVariable Long userId) {
        return service.findByUserSecured(userId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/lessons/{lessonId}/llm-logs")
    public List<LlmLogDto> byLesson(@PathVariable Long lessonId) {
        return service.findByLesson(lessonId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercise-attempts/{attemptId}/llm-logs")
    public List<LlmLogDto> byAttempt(@PathVariable Long attemptId) {
        return service.findByExerciseAttempt(attemptId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/chat-sessions/{sessionId}/llm-logs")
    public List<LlmLogDto> byChatSession(@PathVariable Long sessionId) {
        return service.findByChatSession(sessionId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/llm-logs/{id}")
    public LlmLogDto get(@PathVariable Long id) {
        return service.getSecured(id);
    }
}
