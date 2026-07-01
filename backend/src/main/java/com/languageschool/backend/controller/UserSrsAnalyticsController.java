package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userSrs.SrsThisWeekDto;
import com.languageschool.backend.service.SrsAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserSrsAnalyticsController {

    private final SrsAnalyticsService service;

    public UserSrsAnalyticsController(SrsAnalyticsService service) {
        this.service = service;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/srs/this-week")
    public SrsThisWeekDto thisWeek() {
        return service.getMyThisWeek();
    }
}
