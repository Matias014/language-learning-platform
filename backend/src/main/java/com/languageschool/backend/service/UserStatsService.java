package com.languageschool.backend.service;

import com.languageschool.backend.dto.stats.ActivityStatsDto;
import com.languageschool.backend.dto.stats.EffectivenessStatsDto;

public interface UserStatsService {
    EffectivenessStatsDto getEffectivenessForLogin(String login);

    ActivityStatsDto getActivityForLogin(String login);
}
