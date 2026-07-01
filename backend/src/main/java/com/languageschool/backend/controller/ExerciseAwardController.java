package com.languageschool.backend.controller;

import com.languageschool.backend.dto.exerciseAward.CreateExerciseAwardRequest;
import com.languageschool.backend.dto.exerciseAward.ExerciseAwardDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.ExerciseAwardService;
import com.languageschool.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class ExerciseAwardController {

    private final ExerciseAwardService service;
    private final UserService userService;

    public ExerciseAwardController(ExerciseAwardService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercise-awards")
    public List<ExerciseAwardDto> listAll() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/exercise-awards")
    public ResponseEntity<ExerciseAwardDto> create(@Valid @RequestBody CreateExerciseAwardRequest req) {
        ExerciseAwardDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercise-awards/{id}")
    public ExerciseAwardDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/exercise-awards")
    public List<ExerciseAwardDto> listByUser(@PathVariable Long userId) {
        return service.findByUser(userId);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/exercise-awards")
    public List<ExerciseAwardDto> listMy(Authentication auth) {
        Long me = userService.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return service.findByUser(me);
    }
}
