package com.languageschool.backend.service;

import com.languageschool.backend.dto.adminStats.HardestExerciseDto;
import com.languageschool.backend.dto.adminStats.LlmStatsDto;

import java.util.List;

public interface AdminStatsService {
    LlmStatsDto getLlmStats();

    List<HardestExerciseDto> getHardestExercises(int limit);
}
