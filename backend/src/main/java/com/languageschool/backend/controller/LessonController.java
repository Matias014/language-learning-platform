package com.languageschool.backend.controller;

import com.languageschool.backend.dto.lesson.CreateLessonRequest;
import com.languageschool.backend.dto.lesson.LessonDto;
import com.languageschool.backend.dto.lesson.UpdateLessonRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.LessonService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

import java.util.List;

@RestController
@RequestMapping("/api")
public class LessonController {

    private final LessonService service;

    public LessonController(LessonService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lessons")
    public ResponseEntity<LessonDto> create(@Valid @RequestBody CreateLessonRequest req) {
        LessonDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @GetMapping("/lessons/{id}")
    public LessonDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @GetMapping("/courses/{courseId}/lessons")
    public List<LessonDto> listByCourse(@PathVariable Long courseId) {
        return service.findByCourse(courseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/lessons/{id}")
    public LessonDto update(@PathVariable Long id, @Valid @RequestBody UpdateLessonRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/lessons/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
