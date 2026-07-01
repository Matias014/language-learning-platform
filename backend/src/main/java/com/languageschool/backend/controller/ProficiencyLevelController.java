package com.languageschool.backend.controller;

import com.languageschool.backend.dto.proficiencyLevel.CreateProficiencyLevelRequest;
import com.languageschool.backend.dto.proficiencyLevel.ProficiencyLevelDto;
import com.languageschool.backend.dto.proficiencyLevel.UpdateProficiencyLevelRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.ProficiencyLevelService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class ProficiencyLevelController {

    private final ProficiencyLevelService service;

    public ProficiencyLevelController(ProficiencyLevelService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/proficiency-levels")
    public ResponseEntity<ProficiencyLevelDto> create(@Valid @RequestBody CreateProficiencyLevelRequest req) {
        ProficiencyLevelDto dto = service.create(req);
        return createdAtId(dto.getCode(), dto);
    }

    @GetMapping("/proficiency-levels/{code}")
    public ProficiencyLevelDto get(@PathVariable String code) {
        return service.findByCode(code).orElseThrow(ApiException::notFound);
    }

    @GetMapping("/proficiency-levels")
    public List<ProficiencyLevelDto> list() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/proficiency-levels/{code}")
    public ProficiencyLevelDto update(@PathVariable String code, @Valid @RequestBody UpdateProficiencyLevelRequest req) {
        return service.update(code, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/proficiency-levels/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
