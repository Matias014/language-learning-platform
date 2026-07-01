package com.languageschool.backend.controller;

import com.languageschool.backend.dto.courseRecommendation.CourseRecommendationDto;
import com.languageschool.backend.dto.courseRecommendation.GenerateCourseRecommendationsRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.CourseRecommendationService;
import com.languageschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CourseRecommendationController {

    private final CourseRecommendationService service;
    private final UserService userService;

    public CourseRecommendationController(CourseRecommendationService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/users/me/recommendations/generate")
    public List<CourseRecommendationDto> generate(Authentication auth,
                                                  @Valid @RequestBody GenerateCourseRecommendationsRequest req) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.generateForUser(me, req.getLimit(), req.getLearningLanguageCode(), req.getFromLanguageCode(), req.getLevelCode());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/recommendations/top")
    public List<CourseRecommendationDto> top(@RequestParam(required = false) Integer limit, Authentication auth) {
        Long me = userService.findByLogin(auth.getName()).orElseThrow(ApiException::notFound).getId();
        return service.findTopForUser(me, limit);
    }
}
