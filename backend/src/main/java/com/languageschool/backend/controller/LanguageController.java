package com.languageschool.backend.controller;

import com.languageschool.backend.dto.language.CreateLanguageRequest;
import com.languageschool.backend.dto.language.LanguageDto;
import com.languageschool.backend.dto.language.UpdateLanguageRequest;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.service.LanguageService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.languageschool.backend.util.ControllerUtils.createdAtId;

@RestController
@RequestMapping("/api")
public class LanguageController {

    private final LanguageService service;

    public LanguageController(LanguageService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/languages")
    public ResponseEntity<LanguageDto> create(@Valid @RequestBody CreateLanguageRequest req) {
        LanguageDto dto = service.create(req);
        return createdAtId(dto.getCode(), dto);
    }

    @GetMapping("/languages/{code}")
    public LanguageDto get(@PathVariable String code) {
        return service.findByCode(code).orElseThrow(ApiException::notFound);
    }

    @GetMapping("/languages")
    public List<LanguageDto> list() {
        return service.findAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/languages/{code}")
    public LanguageDto update(@PathVariable String code, @Valid @RequestBody UpdateLanguageRequest req) {
        return service.update(code, req);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/languages/{code}")
    public ResponseEntity<Void> delete(@PathVariable String code) {
        service.delete(code);
        return ResponseEntity.noContent().build();
    }
}
