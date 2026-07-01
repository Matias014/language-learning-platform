package com.languageschool.backend.service;

import com.languageschool.backend.dto.llmLog.LlmLogDto;

import java.util.List;
import java.util.Optional;

public interface LlmLogService {

    Optional<LlmLogDto> findById(Long id);

    List<LlmLogDto> findAll();

    List<LlmLogDto> findByUser(Long userId);

    List<LlmLogDto> findByLesson(Long lessonId);

    List<LlmLogDto> findByExerciseAttempt(Long attemptId);

    List<LlmLogDto> findByChatSession(Long sessionId);

    LlmLogDto getSecured(Long id);

    List<LlmLogDto> findByUserSecured(Long userId);
}
