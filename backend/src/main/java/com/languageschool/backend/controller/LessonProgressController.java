package com.languageschool.backend.controller;

import com.languageschool.backend.dto.lessonProgress.LessonProgressDto;
import com.languageschool.backend.service.LessonProgressService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api", produces = MediaType.APPLICATION_JSON_VALUE)
public class LessonProgressController {

    private final LessonProgressService service;

    public LessonProgressController(LessonProgressService service) {
        this.service = service;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/lessons/{lessonId}/progress-percent")
    public LessonProgressDto myForLesson(@PathVariable Long lessonId, Authentication authentication) {
        return service.getMyForLesson(lessonId, authentication.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/courses/{courseId}/lessons/progress")
    public List<LessonProgressDto> myLessonsInCourse(@PathVariable Long courseId, Authentication authentication) {
        return service.listMyByCourse(courseId, authentication.getName());
    }
}
