package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userSrs.ReviewUserSrsRequest;
import com.languageschool.backend.dto.userSrs.UserSrsDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserService;
import com.languageschool.backend.service.UserSrsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
public class UserSrsController {

    private final UserSrsService service;
    private final UserService userService;

    public UserSrsController(UserSrsService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/srs")
    public List<UserSrsDto> list(Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findByUser(me);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/srs/due")
    public List<UserSrsDto> due(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant before,
                                Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findDueByUser(me, before);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/users/me/srs/review")
    public UserSrsDto review(@Valid @RequestBody ReviewUserSrsRequest req, Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.review(me, req.getExerciseId(), req.getQuality());
    }
}
