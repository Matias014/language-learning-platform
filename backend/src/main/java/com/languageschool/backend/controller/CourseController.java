package com.languageschool.backend.controller;

import com.languageschool.backend.dto.course.CourseDto;
import com.languageschool.backend.dto.course.CreateCourseRequest;
import com.languageschool.backend.dto.course.UpdateCourseRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.CourseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService service;

    public CourseController(CourseService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/courses")
    public ResponseEntity<CourseDto> create(@Valid @RequestBody CreateCourseRequest req) {
        CourseDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @GetMapping("/courses/{id}")
    public CourseDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @GetMapping("/courses")
    public List<CourseDto> list(@RequestParam(required = false) String learningLanguageCode,
                                @RequestParam(required = false) String fromLanguageCode,
                                @RequestParam(required = false) String levelCode) {
        if (learningLanguageCode == null && fromLanguageCode == null && levelCode == null) {
            return service.findAll();
        }
        return service.findBy(learningLanguageCode, fromLanguageCode, levelCode);
    }

    @GetMapping("/courses/batch")
    public List<CourseDto> batch(@RequestParam(name = "ids", required = false) String ids) {
        if (ids == null || ids.isBlank()) {
            return List.of();
        }
        Set<Long> unique = new LinkedHashSet<>();
        for (String s : ids.split(",")) {
            String t = s.trim();
            if (!t.isEmpty()) {
                try {
                    unique.add(Long.parseLong(t));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (unique.isEmpty()) {
            return List.of();
        }
        return unique.stream()
                .map(service::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/courses/{id}")
    public CourseDto update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/courses/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
