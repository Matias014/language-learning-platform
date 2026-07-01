package com.languageschool.backend.controller;

import com.languageschool.backend.dto.exercise.CreateExerciseRequest;
import com.languageschool.backend.dto.exercise.ExerciseDto;
import com.languageschool.backend.dto.exercise.UpdateExerciseRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.ExerciseService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ExerciseController {

    private final ExerciseService service;

    public ExerciseController(ExerciseService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/exercises")
    public ResponseEntity<ExerciseDto> create(@Valid @RequestBody CreateExerciseRequest req) {
        ExerciseDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exercises/{id}")
    public ExerciseDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/lessons/{lessonId}/exercises")
    public List<ExerciseDto> listByLesson(@PathVariable Long lessonId) {
        return service.findByLesson(lessonId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/exercises/{id}")
    public ExerciseDto update(@PathVariable Long id, @Valid @RequestBody UpdateExerciseRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/exercises/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
