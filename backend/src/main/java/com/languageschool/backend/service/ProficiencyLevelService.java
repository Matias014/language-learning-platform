package com.languageschool.backend.service;

import com.languageschool.backend.dto.proficiencyLevel.CreateProficiencyLevelRequest;
import com.languageschool.backend.dto.proficiencyLevel.ProficiencyLevelDto;
import com.languageschool.backend.dto.proficiencyLevel.UpdateProficiencyLevelRequest;

import java.util.List;
import java.util.Optional;

public interface ProficiencyLevelService {

    ProficiencyLevelDto create(CreateProficiencyLevelRequest req);

    Optional<ProficiencyLevelDto> findByCode(String code);

    List<ProficiencyLevelDto> findAll();

    ProficiencyLevelDto update(String code, UpdateProficiencyLevelRequest req);

    void delete(String code);
}
