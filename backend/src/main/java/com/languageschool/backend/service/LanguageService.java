package com.languageschool.backend.service;

import com.languageschool.backend.dto.language.CreateLanguageRequest;
import com.languageschool.backend.dto.language.LanguageDto;
import com.languageschool.backend.dto.language.UpdateLanguageRequest;

import java.util.List;
import java.util.Optional;

public interface LanguageService {

    LanguageDto create(CreateLanguageRequest req);

    Optional<LanguageDto> findByCode(String code);

    List<LanguageDto> findAll();

    LanguageDto update(String code, UpdateLanguageRequest req);

    void delete(String code);
}
