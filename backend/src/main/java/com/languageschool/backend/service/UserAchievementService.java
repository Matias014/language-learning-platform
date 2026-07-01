package com.languageschool.backend.service;

import com.languageschool.backend.dto.userAchievement.CreateUserAchievementRequest;
import com.languageschool.backend.dto.userAchievement.UserAchievementDto;

import java.util.List;
import java.util.Optional;

public interface UserAchievementService {

    UserAchievementDto create(CreateUserAchievementRequest req);

    UserAchievementDto getById(Long id);

    Optional<UserAchievementDto> findByUserAndAchievement(Long userId, Long achievementId);

    List<UserAchievementDto> findByUser(Long userId);

    List<UserAchievementDto> findByAchievement(Long achievementId);

    void delete(Long id);

    UserAchievementDto getSecured(Long id);

    List<UserAchievementDto> findByUserSecured(Long userId);

    Optional<UserAchievementDto> getByUserAndAchievementSecured(Long userId, Long achievementId);

    List<UserAchievementDto> findAll();
}
