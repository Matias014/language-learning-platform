package com.languageschool.backend.controller;

import com.languageschool.backend.dto.courseEnrollment.CourseEnrollmentDto;
import com.languageschool.backend.dto.courseEnrollment.CreateCourseEnrollmentRequest;
import com.languageschool.backend.dto.courseEnrollment.UpdateCourseEnrollmentRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.CourseEnrollmentService;
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
public class CourseEnrollmentController {

    private final CourseEnrollmentService service;
    private final UserService userService;

    public CourseEnrollmentController(CourseEnrollmentService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/enrollments")
    public List<CourseEnrollmentDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/enrollments")
    public ResponseEntity<CourseEnrollmentDto> create(@Valid @RequestBody CreateCourseEnrollmentRequest req) {
        CourseEnrollmentDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/enrollments/{id}")
    public CourseEnrollmentDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/enrollments")
    public List<CourseEnrollmentDto> listByUser(@PathVariable Long userId) {
        return service.findByUser(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/enrollments")
    public List<CourseEnrollmentDto> listMy(Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findByUser(me);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/courses/{courseId}/enrollments")
    public List<CourseEnrollmentDto> listByCourse(@PathVariable Long courseId) {
        return service.findByCourse(courseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/courses/{courseId}/enrollment")
    public CourseEnrollmentDto getByUserAndCourse(@PathVariable Long userId, @PathVariable Long courseId) {
        return service.findByUserAndCourse(userId, courseId).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/courses/{courseId}/enrollment")
    public CourseEnrollmentDto getMyByCourse(@PathVariable Long courseId, Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findByUserAndCourse(me, courseId).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @PatchMapping("/enrollments/{id}")
    public CourseEnrollmentDto update(@PathVariable Long id, @Valid @RequestBody UpdateCourseEnrollmentRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("isAuthenticated()")
    @DeleteMapping("/enrollments/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
