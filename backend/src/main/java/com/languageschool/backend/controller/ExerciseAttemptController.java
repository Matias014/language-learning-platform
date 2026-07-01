package com.languageschool.backend.controller;

import com.languageschool.backend.dto.ai.GradeResponse;
import com.languageschool.backend.dto.exerciseAttempt.CreateExerciseAttemptRequest;
import com.languageschool.backend.dto.exerciseAttempt.ExerciseAttemptDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.AiGradingService;
import com.languageschool.backend.service.ExerciseAttemptService;
import com.languageschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class ExerciseAttemptController {

    private final ExerciseAttemptService service;
    private final UserService userService;
    private final ObjectProvider<AiGradingService> gradingProvider;

    public ExerciseAttemptController(ExerciseAttemptService service,
                                     UserService userService,
                                     ObjectProvider<AiGradingService> gradingProvider) {
        this.service = service;
        this.userService = userService;
        this.gradingProvider = gradingProvider;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercise-attempts")
    public List<ExerciseAttemptDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercises/{exerciseId}/attempts")
    public List<ExerciseAttemptDto> listByExercise(@PathVariable Long exerciseId) {
        return service.findByExercise(exerciseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/exercises/{exerciseId}/attempts")
    public List<ExerciseAttemptDto> listByUserAndExercise(@PathVariable Long userId,
                                                          @PathVariable Long exerciseId) {
        return service.findByUserAndExercise(userId, exerciseId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercise-attempts/{id}")
    public ExerciseAttemptDto get(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/exercises/{exerciseId}/attempts")
    public List<ExerciseAttemptDto> myAttemptsByExercise(@PathVariable Long exerciseId,
                                                         Authentication authentication) {
        Long me = userService.findByLogin(authentication.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return service.findByUserAndExercise(me, exerciseId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/exercises/{exerciseId}/attempts/latest")
    public ExerciseAttemptDto myLatestByExercise(@PathVariable Long exerciseId,
                                                 Authentication authentication) {
        Long me = userService.findByLogin(authentication.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return service.findLatestByUserAndExercise(me, exerciseId)
                .orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/exercises/{exerciseId}/attempts/latest")
    public ExerciseAttemptDto latestByUserAndExercise(@PathVariable Long userId,
                                                      @PathVariable Long exerciseId) {
        return service.findLatestByUserAndExercise(userId, exerciseId)
                .orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/exercise-attempts")
    public ResponseEntity<ExerciseAttemptDto> create(@Valid @RequestBody CreateExerciseAttemptRequest request,
                                                     Authentication authentication) {
        Long me = userService.findByLogin(authentication.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        ExerciseAttemptDto dto = service.create(me, request);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/exercise-attempts/{id}/evaluations")
    public GradeResponse evaluate(@PathVariable Long id, Authentication authentication) {
        AiGradingService grading = gradingProvider.getIfAvailable();
        if (grading == null) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.AI_GRADING_DISABLED);
        }
        return grading.gradeAttempt(id, authentication.getName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/exercise-attempts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
