package com.languageschool.backend.controller;

import com.languageschool.backend.dto.stats.ActivityStatsDto;
import com.languageschool.backend.dto.stats.EffectivenessStatsDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserService;
import com.languageschool.backend.service.UserStatsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserStatsController {

    private final UserStatsService stats;
    private final UserService users;

    public UserStatsController(UserStatsService stats, UserService users) {
        this.stats = stats;
        this.users = users;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/stats/effectiveness")
    public EffectivenessStatsDto effectiveness(Authentication auth) {
        String login = auth.getName();
        users.findByLogin(login).orElseThrow(ApiException::notFound);
        return stats.getEffectivenessForLogin(login);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/stats/activity")
    public ActivityStatsDto activity(Authentication auth) {
        String login = auth.getName();
        users.findByLogin(login).orElseThrow(ApiException::notFound);
        return stats.getActivityForLogin(login);
    }
}
