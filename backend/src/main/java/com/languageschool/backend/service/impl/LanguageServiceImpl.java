package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.language.CreateLanguageRequest;
import com.languageschool.backend.dto.language.LanguageDto;
import com.languageschool.backend.dto.language.UpdateLanguageRequest;
import com.languageschool.backend.entity.Language;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.LanguageRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.LanguageService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class LanguageServiceImpl implements LanguageService {

    private final LanguageRepository repo;

    public LanguageServiceImpl(LanguageRepository repo) {
        this.repo = repo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Transactional
    @Override
    public LanguageDto create(CreateLanguageRequest req) {
        SecurityUtils.requireAdmin(auth());

        String code = trim(req.getCode());
        String name = trim(req.getName());

        if (code == null || code.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.CODE_REQUIRED);
        }
        if (repo.existsById(code)) {
            throw ApiException.conflict(ErrorCode.CODE_TAKEN);
        }

        Language l = new Language();
        l.setCode(code);
        l.setName(name);
        return toDto(repo.save(l));
    }

    @Override
    public Optional<LanguageDto> findByCode(String code) {
        return repo.findById(code).map(this::toDto);
    }

    @Override
    public List<LanguageDto> findAll() {
        return repo.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public LanguageDto update(String code, UpdateLanguageRequest req) {
        SecurityUtils.requireAdmin(auth());

        Language l = repo.findById(code)
                .orElseThrow(ApiException::notFound);

        if (req.getName() != null) {
            l.setName(trim(req.getName()));
        }

        return toDto(repo.save(l));
    }

    @Transactional
    @Override
    public void delete(String code) {
        SecurityUtils.requireAdmin(auth());

        Language l = repo.findById(code)
                .orElseThrow(ApiException::notFound);
        repo.delete(l);
    }

    private LanguageDto toDto(Language l) {
        return new LanguageDto(l.getCode(), l.getName());
    }
}
