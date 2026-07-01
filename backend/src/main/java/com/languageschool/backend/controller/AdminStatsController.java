package com.languageschool.backend.controller;

import com.languageschool.backend.dto.adminStats.HardestExerciseDto;
import com.languageschool.backend.dto.adminStats.LlmStatsDto;
import com.languageschool.backend.service.AdminStatsService;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/stats")
public class AdminStatsController {

    private final AdminStatsService service;

    public AdminStatsController(AdminStatsService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/llm")
    public LlmStatsDto llm() {
        return service.getLlmStats();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/exercises/hardest")
    public List<HardestExerciseDto> hardest(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return service.getHardestExercises(limit);
    }
}
