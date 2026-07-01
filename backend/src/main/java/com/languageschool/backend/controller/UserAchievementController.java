package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userAchievement.CreateUserAchievementRequest;
import com.languageschool.backend.dto.userAchievement.UserAchievementDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserAchievementService;
import com.languageschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class UserAchievementController {

    private final UserAchievementService service;
    private final UserService userService;

    public UserAchievementController(UserAchievementService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user-achievements")
    public List<UserAchievementDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/user-achievements")
    public ResponseEntity<UserAchievementDto> create(@Valid @RequestBody CreateUserAchievementRequest req) {
        UserAchievementDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user-achievements/{id}")
    public UserAchievementDto get(@PathVariable Long id) {
        return service.getSecured(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/achievements")
    public List<UserAchievementDto> listByUser(@PathVariable Long userId) {
        return service.findByUserSecured(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/achievements")
    public List<UserAchievementDto> listMy(Authentication auth) {
        Long me = userService.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return service.findByUserSecured(me);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/achievements/{achievementId}/user-achievements")
    public List<UserAchievementDto> listByAchievement(@PathVariable Long achievementId) {
        return service.findByAchievement(achievementId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/achievements/{achievementId}")
    public UserAchievementDto getByUserAndAchievement(@PathVariable Long userId,
                                                      @PathVariable Long achievementId) {
        return service.getByUserAndAchievementSecured(userId, achievementId)
                .orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/achievements/{achievementId}")
    public UserAchievementDto getMyByAchievement(@PathVariable Long achievementId,
                                                 Authentication auth) {
        Long me = userService.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return service.getByUserAndAchievementSecured(me, achievementId)
                .orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/user-achievements/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
