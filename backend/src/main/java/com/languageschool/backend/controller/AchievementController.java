package com.languageschool.backend.controller;

import com.languageschool.backend.dto.achievement.AchievementDto;
import com.languageschool.backend.dto.achievement.CreateAchievementRequest;
import com.languageschool.backend.dto.achievement.UpdateAchievementRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class AchievementController {

    private final AchievementService service;

    public AchievementController(AchievementService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/achievements")
    public ResponseEntity<AchievementDto> create(@Valid @RequestBody CreateAchievementRequest req) {
        AchievementDto dto = service.create(req);
        return createdAtId(dto.getId(), dto);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/achievements/{id}")
    public AchievementDto get(@PathVariable Long id) {
        return service.findById(id).orElseThrow(ApiException::notFound);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/achievements")
    public List<AchievementDto> list() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/achievements/{id}")
    public AchievementDto update(@PathVariable Long id, @Valid @RequestBody UpdateAchievementRequest req) {
        return service.update(id, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/achievements/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
