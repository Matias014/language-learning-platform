package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userLessonProgress.CreateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UpdateUserLessonProgressRequest;
import com.languageschool.backend.dto.userLessonProgress.UserLessonProgressDto;
import com.languageschool.backend.entity.LessonStatus;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserLessonProgressService;
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
public class UserLessonProgressController {

    private final UserLessonProgressService service;
    private final UserService userService;

    public UserLessonProgressController(UserLessonProgressService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user-lesson-progress")
    public List<UserLessonProgressDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/user-lesson-progress")
    public ResponseEntity<UserLessonProgressDto> create(@Valid @RequestBody CreateUserLessonProgressRequest req) {
        UserLessonProgressDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/user-lesson-progress/{id}")
    public UserLessonProgressDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/lesson-progress")
    public List<UserLessonProgressDto> listByUser(@PathVariable Long userId,
                                                  @RequestParam(name = "status", required = false) LessonStatus status) {
        return status == null ? service.findByUser(userId) : service.findByUser(userId, status);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/lesson-progress")
    public List<UserLessonProgressDto> listMy(Authentication auth,
                                              @RequestParam(name = "status", required = false) LessonStatus status) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return status == null ? service.findByUser(me) : service.findByUser(me, status);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/lessons/{lessonId}/progress")
    public UserLessonProgressDto getByUserAndLesson(@PathVariable Long userId, @PathVariable Long lessonId) {
        return service.findByUserAndLesson(userId, lessonId).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/lessons/{lessonId}/progress")
    public UserLessonProgressDto getMyByLesson(@PathVariable Long lessonId, Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findByUserAndLesson(me, lessonId).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/user-lesson-progress/{id}")
    public UserLessonProgressDto update(@PathVariable Long id, @Valid @RequestBody UpdateUserLessonProgressRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/user-lesson-progress/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
