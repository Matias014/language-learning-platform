package com.languageschool.backend.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String storeUserAvatar(Long userId, MultipartFile file);

    String storeCourseCountryIcon(Long courseId, MultipartFile file);

    String storeAchievementIcon(Long achievementId, MultipartFile file);

    Resource loadAvatar(String filename);

    Resource loadCountryIcon(String filename);

    Resource loadAchievementIcon(String filename);
}
