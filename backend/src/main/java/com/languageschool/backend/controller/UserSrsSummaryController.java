package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userSrs.SrsSummaryDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.SrsSummaryService;
import com.languageschool.backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserSrsSummaryController {

    private final SrsSummaryService service;
    private final UserService users;

    public UserSrsSummaryController(SrsSummaryService service, UserService users) {
        this.service = service;
        this.users = users;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/srs/summary")
    public SrsSummaryDto summary(Authentication auth) {
        String login = auth.getName();
        users.findByLogin(login).orElseThrow(ApiException::notFound);
        return service.getSummaryForLogin(login);
    }
}
