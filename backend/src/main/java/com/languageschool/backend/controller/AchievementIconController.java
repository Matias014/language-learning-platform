package com.languageschool.backend.controller;

import com.languageschool.backend.dto.achievement.AchievementDto;
import com.languageschool.backend.dto.achievement.UpdateAchievementRequest;
import com.languageschool.backend.service.AchievementService;
import com.languageschool.backend.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class AchievementIconController {

    private final FileStorageService storage;
    private final AchievementService achievementService;

    public AchievementIconController(FileStorageService storage, AchievementService achievementService) {
        this.storage = storage;
        this.achievementService = achievementService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/achievements/{id}/icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AchievementDto upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        String publicPath = storage.storeAchievementIcon(id, file);
        UpdateAchievementRequest req = UpdateAchievementRequest.builder()
                .iconPath(publicPath)
                .build();
        return achievementService.update(id, req);
    }
}
