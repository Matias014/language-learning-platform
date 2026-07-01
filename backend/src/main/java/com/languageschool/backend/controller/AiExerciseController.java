package com.languageschool.backend.controller;

import com.languageschool.backend.dto.ai.AiExerciseDtos.GenerateExercisesRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.AiExerciseService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AiExerciseController {

    private final ObjectProvider<AiExerciseService> aiExerciseServiceProvider;

    public AiExerciseController(ObjectProvider<AiExerciseService> aiExerciseServiceProvider) {
        this.aiExerciseServiceProvider = aiExerciseServiceProvider;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/lessons/{lessonId}/exercises")
    public ResponseEntity<List<Long>> generate(@PathVariable Long lessonId,
                                               @Valid @RequestBody GenerateExercisesRequest req) {
        AiExerciseService service = aiExerciseServiceProvider.getIfAvailable();
        if (service == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AI_DISABLED);
        }
        List<Long> ids = service.generate(
                lessonId,
                req.exerciseType(),
                req.difficultyLevel(),
                req.topic(),
                req.count(),
                req.xp()
        );
        if (ids == null || ids.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/exercises/{id}")
                .buildAndExpand(ids.get(0))
                .toUri();
        return ResponseEntity.created(location).body(ids);
    }
}
