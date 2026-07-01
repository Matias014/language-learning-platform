package com.languageschool.backend.service;

import com.languageschool.backend.dto.userLevel.CreateLevelRequest;
import com.languageschool.backend.dto.userLevel.LevelDto;
import com.languageschool.backend.dto.userLevel.UpdateLevelRequest;

import java.util.List;
import java.util.Optional;

public interface UserLevelService {

    LevelDto create(CreateLevelRequest req);

    Optional<LevelDto> findById(Integer level);

    List<LevelDto> findAll();

    LevelDto update(Integer level, UpdateLevelRequest req);

    void delete(Integer level);

    // pomocnicze (opcjonalnie): wyznacz poziom po XP
    Integer resolveLevelForXp(int totalXp);
}
