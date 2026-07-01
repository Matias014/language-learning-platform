package com.languageschool.backend.controller;

import com.languageschool.backend.dto.xp.XpBreakdownDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.XpAnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class XpAnalyticsController {

    private final XpAnalyticsService service;

    public XpAnalyticsController(XpAnalyticsService service) {
        this.service = service;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/xp/breakdown")
    public XpBreakdownDto breakdown(@RequestParam(name = "days", defaultValue = "7") int days) {
        validateDays(days);
        return service.getMyBreakdown(days);
    }

    private void validateDays(int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw ApiException.badRequest(ErrorCode.INVALID_DAYS);
        }
    }
}
