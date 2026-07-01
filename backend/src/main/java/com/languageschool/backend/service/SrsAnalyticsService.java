package com.languageschool.backend.service;

import com.languageschool.backend.dto.userSrs.SrsThisWeekDto;

public interface SrsAnalyticsService {
    SrsThisWeekDto getMyThisWeek();
}
