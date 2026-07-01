package com.languageschool.backend.service;

import com.languageschool.backend.dto.achievement.AchievementDto;
import com.languageschool.backend.dto.achievement.CreateAchievementRequest;
import com.languageschool.backend.dto.achievement.UpdateAchievementRequest;

import java.util.List;
import java.util.Optional;

public interface AchievementService {

    AchievementDto create(CreateAchievementRequest req);

    Optional<AchievementDto> findById(Long id);

    List<AchievementDto> findAll();

    AchievementDto update(Long id, UpdateAchievementRequest req);

    void delete(Long id);
}

