package com.languageschool.backend.controller;

import com.languageschool.backend.dto.exerciseOption.CreateExerciseOptionRequest;
import com.languageschool.backend.dto.exerciseOption.ExerciseOptionDto;
import com.languageschool.backend.dto.exerciseOption.UpdateExerciseOptionRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.ExerciseOptionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ExerciseOptionController {

    private final ExerciseOptionService service;

    public ExerciseOptionController(ExerciseOptionService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/exercise-options")
    public ResponseEntity<ExerciseOptionDto> create(@Valid @RequestBody CreateExerciseOptionRequest req) {
        ExerciseOptionDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exercise-options/{id}")
    public ExerciseOptionDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/exercises/{exerciseId}/options")
    public List<ExerciseOptionDto> listByExercise(@PathVariable Long exerciseId) {
        return service.findByExercise(exerciseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/exercise-options/{id}")
    public ExerciseOptionDto update(@PathVariable Long id, @Valid @RequestBody UpdateExerciseOptionRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/exercise-options/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
