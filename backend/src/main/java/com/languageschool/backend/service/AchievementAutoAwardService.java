package com.languageschool.backend.service;

public interface AchievementAutoAwardService {
    void awardMissingForUserId(Long userId);
}
