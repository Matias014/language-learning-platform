package com.languageschool.backend.service;

import com.languageschool.backend.dto.ai.GradeResponse;

public interface AiGradingService {
    GradeResponse gradeAttempt(Long attemptId, String login);
}
