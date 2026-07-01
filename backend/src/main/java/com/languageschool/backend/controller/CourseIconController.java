package com.languageschool.backend.controller;

import com.languageschool.backend.dto.course.CourseDto;
import com.languageschool.backend.service.CourseService;
import com.languageschool.backend.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class CourseIconController {

    private final FileStorageService storage;
    private final CourseService courseService;

    public CourseIconController(FileStorageService storage, CourseService courseService) {
        this.storage = storage;
        this.courseService = courseService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/courses/{id}/country-icon", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CourseDto upload(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        String publicPath = storage.storeCourseCountryIcon(id, file);
        return courseService.updateCountryIconPath(id, publicPath);
    }
}
