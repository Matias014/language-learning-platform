package com.languageschool.backend.service;

import com.languageschool.backend.dto.userLevel.LevelSummaryDto;

public interface LevelSummaryService {
    LevelSummaryDto getSummaryForLogin(String login);
}
