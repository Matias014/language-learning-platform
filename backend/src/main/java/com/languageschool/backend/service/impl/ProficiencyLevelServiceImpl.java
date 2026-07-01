package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.proficiencyLevel.CreateProficiencyLevelRequest;
import com.languageschool.backend.dto.proficiencyLevel.ProficiencyLevelDto;
import com.languageschool.backend.dto.proficiencyLevel.UpdateProficiencyLevelRequest;
import com.languageschool.backend.entity.ProficiencyLevel;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.ProficiencyLevelRepository;
import com.languageschool.backend.service.ProficiencyLevelService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProficiencyLevelServiceImpl implements ProficiencyLevelService {

    private final ProficiencyLevelRepository repo;

    public ProficiencyLevelServiceImpl(ProficiencyLevelRepository repo) {
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
    public ProficiencyLevelDto create(CreateProficiencyLevelRequest req) {
        SecurityUtils.requireAdmin(auth());

        String code = trim(req.getCode());
        String name = trim(req.getName());

        if (code == null || code.isEmpty()) {
            throw ApiException.badRequest(ErrorCode.CODE_REQUIRED);
        }
        if (repo.existsById(code)) {
            throw ApiException.conflict(ErrorCode.CODE_TAKEN);
        }
        if (req.getOrderNumber() == null || req.getOrderNumber() < 1) {
            throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
        }

        ProficiencyLevel pl = new ProficiencyLevel();
        pl.setCode(code);
        pl.setName(name);
        pl.setOrderNumber(req.getOrderNumber());

        try {
            return toDto(repo.save(pl));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Override
    public Optional<ProficiencyLevelDto> findByCode(String code) {
        return repo.findById(code).map(this::toDto);
    }

    @Override
    public List<ProficiencyLevelDto> findAll() {
        return repo.findAll().stream()
                .sorted(Comparator
                        .comparing(ProficiencyLevel::getOrderNumber,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(ProficiencyLevel::getCode))
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public ProficiencyLevelDto update(String code, UpdateProficiencyLevelRequest req) {
        SecurityUtils.requireAdmin(auth());

        ProficiencyLevel pl = repo.findById(code)
                .orElseThrow(ApiException::notFound);

        if (req.getName() != null) {
            pl.setName(trim(req.getName()));
        }
        if (req.getOrderNumber() != null) {
            if (req.getOrderNumber() < 1) {
                throw ApiException.badRequest(ErrorCode.ORDER_NUMBER_MIN_1);
            }
            pl.setOrderNumber(req.getOrderNumber());
        }

        try {
            return toDto(repo.save(pl));
        } catch (DataIntegrityViolationException e) {
            throw ApiException.conflict(ErrorCode.ORDER_NUMBER_TAKEN);
        }
    }

    @Transactional
    @Override
    public void delete(String code) {
        SecurityUtils.requireAdmin(auth());

        ProficiencyLevel pl = repo.findById(code)
                .orElseThrow(ApiException::notFound);
        repo.delete(pl);
    }

    private ProficiencyLevelDto toDto(ProficiencyLevel pl) {
        return new ProficiencyLevelDto(pl.getCode(), pl.getName(), pl.getOrderNumber());
    }
}
