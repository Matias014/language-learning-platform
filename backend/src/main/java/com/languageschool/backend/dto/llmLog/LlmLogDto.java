package com.languageschool.backend.dto.llmLog;

import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@AllArgsConstructor
@Builder
public class LlmLogDto {
    Long id;
    Long userId;
    Long lessonId;
    Long exerciseAttemptId;
    Long chatSessionId;
    InteractionType interactionType;
    String model;
    Integer tokensIn;
    Integer tokensOut;
    Integer latencyMs;
    Map<String, Object> params;
    LlmStatus status;
    String prompt;
    String response;
    Instant createdAt;
}
