package com.languageschool.backend.controller;

import com.languageschool.backend.dto.courseProgress.CourseProgressDto;
import com.languageschool.backend.service.CourseProgressService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class CourseProgressController {

    private final CourseProgressService service;

    public CourseProgressController(CourseProgressService service) {
        this.service = service;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/courses/progress")
    public List<CourseProgressDto> listMy(Authentication authentication) {
        return service.listMy(authentication.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/courses/{courseId}/progress")
    public CourseProgressDto myForCourse(@PathVariable Long courseId, Authentication authentication) {
        return service.getMyForCourse(courseId, authentication.getName());
    }
}
