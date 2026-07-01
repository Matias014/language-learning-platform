package com.languageschool.backend.service;

import com.languageschool.backend.dto.xp.XpBreakdownDto;

public interface XpAnalyticsService {
    XpBreakdownDto getMyBreakdown(int days);
}
