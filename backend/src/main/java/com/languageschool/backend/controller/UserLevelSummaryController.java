package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userLevel.LevelSummaryDto;
import com.languageschool.backend.service.LevelSummaryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserLevelSummaryController {

    private final LevelSummaryService service;

    public UserLevelSummaryController(LevelSummaryService service) {
        this.service = service;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/level-summary")
    public LevelSummaryDto summary(Authentication auth) {
        return service.getSummaryForLogin(auth.getName());
    }
}
