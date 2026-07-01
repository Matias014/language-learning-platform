package com.languageschool.backend.controller;

import com.languageschool.backend.dto.userLevel.CreateLevelRequest;
import com.languageschool.backend.dto.userLevel.LevelDto;
import com.languageschool.backend.dto.userLevel.UpdateLevelRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.UserLevelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class UserLevelController {

    private final UserLevelService service;

    public UserLevelController(UserLevelService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/levels")
    public ResponseEntity<LevelDto> create(@Valid @RequestBody CreateLevelRequest req) {
        LevelDto dto = service.create(req);
        return createdAtId(dto.getLevel(), dto);
    }

    @GetMapping("/levels/{level}")
    public LevelDto get(@PathVariable Integer level) {
        return service.findById(level).orElseThrow(ApiException::notFound);
    }

    @GetMapping("/levels")
    public List<LevelDto> list() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/levels/{level}")
    public LevelDto update(@PathVariable Integer level, @Valid @RequestBody UpdateLevelRequest req) {
        return service.update(level, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/levels/{level}")
    public ResponseEntity<Void> delete(@PathVariable Integer level) {
        service.delete(level);
        return ResponseEntity.noContent().build();
    }
}
