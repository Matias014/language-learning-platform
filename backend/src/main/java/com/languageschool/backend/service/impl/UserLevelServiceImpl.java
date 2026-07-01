package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userLevel.CreateLevelRequest;
import com.languageschool.backend.dto.userLevel.LevelDto;
import com.languageschool.backend.dto.userLevel.UpdateLevelRequest;
import com.languageschool.backend.entity.UserLevel;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.UserLevelRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.UserLevelService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserLevelServiceImpl implements UserLevelService {

    private final UserLevelRepository repo;

    public UserLevelServiceImpl(UserLevelRepository repo) {
        this.repo = repo;
    }

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Transactional
    @Override
    public LevelDto create(CreateLevelRequest req) {
        SecurityUtils.requireAdmin(auth());

        if (req.getLevel() == null || req.getLevel() < 1) {
            throw ApiException.badRequest(ErrorCode.LEVEL_POSITIVE);
        }
        if (req.getRequiredXp() == null || req.getRequiredXp() < 0) {
            throw ApiException.badRequest(ErrorCode.REQUIRED_XP_NON_NEGATIVE);
        }
        if (repo.existsById(req.getLevel())) {
            throw ApiException.conflict(ErrorCode.LEVEL_TAKEN);
        }
        UserLevel l = new UserLevel(req.getLevel(), req.getRequiredXp());
        return toDto(repo.save(l));
    }

    @Override
    public Optional<LevelDto> findById(Integer level) {
        return repo.findById(level).map(this::toDto);
    }

    @Override
    public List<LevelDto> findAll() {
        return repo.findAll(Sort.by(Sort.Direction.ASC, "level"))
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public LevelDto update(Integer level, UpdateLevelRequest req) {
        SecurityUtils.requireAdmin(auth());

        UserLevel l = repo.findById(level)
                .orElseThrow(ApiException::notFound);
        if (req.getRequiredXp() != null) {
            if (req.getRequiredXp() < 0) {
                throw ApiException.badRequest(ErrorCode.REQUIRED_XP_NON_NEGATIVE);
            }
            l.setRequiredXp(req.getRequiredXp());
        }
        return toDto(repo.save(l));
    }

    @Transactional
    @Override
    public void delete(Integer level) {
        SecurityUtils.requireAdmin(auth());

        UserLevel l = repo.findById(level)
                .orElseThrow(ApiException::notFound);
        repo.delete(l);
    }

    @Override
    public Integer resolveLevelForXp(int totalXp) {
        return repo.findByRequiredXpLessThanEqualOrderByLevelDesc(totalXp)
                .stream()
                .findFirst()
                .map(UserLevel::getLevel)
                .orElse(1);
    }

    private LevelDto toDto(UserLevel l) {
        return new LevelDto(l.getLevel(), l.getRequiredXp());
    }
}
