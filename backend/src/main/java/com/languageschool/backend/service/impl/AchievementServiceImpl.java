package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.achievement.AchievementDto;
import com.languageschool.backend.dto.achievement.CreateAchievementRequest;
import com.languageschool.backend.dto.achievement.UpdateAchievementRequest;
import com.languageschool.backend.entity.Achievement;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.AchievementRepository;
import com.languageschool.backend.util.SecurityUtils;
import com.languageschool.backend.service.AchievementService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository repo;

    public AchievementServiceImpl(AchievementRepository repo) {
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
    public AchievementDto create(CreateAchievementRequest req) {
        SecurityUtils.requireAdmin(auth());

        String title = trim(req.getTitle());
        if (title == null || title.isBlank()) {
            throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
        }
        if (repo.findByTitle(title).isPresent()) {
            throw ApiException.conflict(ErrorCode.TITLE_TAKEN);
        }
        if (req.getRequiredXp() != null && req.getRequiredXp() < 0) {
            throw ApiException.badRequest(ErrorCode.REQUIRED_XP_NON_NEGATIVE);
        }
        Achievement a = new Achievement();
        a.setTitle(title);
        a.setDescription(trim(req.getDescription()));
        a.setIconPath(trim(req.getIconPath()));
        a.setRequiredXp(req.getRequiredXp());
        return toDto(repo.save(a));
    }

    @Override
    public Optional<AchievementDto> findById(Long id) {
        return repo.findById(id).map(this::toDto);
    }

    @Override
    public List<AchievementDto> findAll() {
        Sort sort = Sort.by(Sort.Order.asc("requiredXp"), Sort.Order.asc("title"));
        return repo.findAll(sort).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public AchievementDto update(Long id, UpdateAchievementRequest req) {
        SecurityUtils.requireAdmin(auth());

        Achievement a = repo.findById(id)
                .orElseThrow(ApiException::notFound);

        if (req.getTitle() != null) {
            String t = trim(req.getTitle());
            if (t == null || t.isBlank()) {
                throw ApiException.badRequest(ErrorCode.TITLE_REQUIRED);
            }
            if (!t.equals(a.getTitle()) && repo.findByTitle(t).isPresent()) {
                throw ApiException.conflict(ErrorCode.TITLE_TAKEN);
            }
            a.setTitle(t);
        }
        if (req.getDescription() != null) {
            a.setDescription(trim(req.getDescription()));
        }
        if (req.getIconPath() != null) {
            a.setIconPath(trim(req.getIconPath()));
        }
        if (req.getRequiredXp() != null) {
            if (req.getRequiredXp() < 0) {
                throw ApiException.badRequest(ErrorCode.REQUIRED_XP_NON_NEGATIVE);
            }
            a.setRequiredXp(req.getRequiredXp());
        }
        return toDto(repo.save(a));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        SecurityUtils.requireAdmin(auth());
        if (!repo.existsById(id)) {
            throw ApiException.notFound();
        }
        repo.deleteById(id);
    }

    private AchievementDto toDto(Achievement a) {
        return AchievementDto.builder()
                .id(a.getId())
                .title(a.getTitle())
                .description(a.getDescription())
                .iconPath(a.getIconPath())
                .requiredXp(a.getRequiredXp())
                .build();
    }
}
