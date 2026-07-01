package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userAchievement.CreateUserAchievementRequest;
import com.languageschool.backend.dto.userAchievement.UserAchievementDto;
import com.languageschool.backend.entity.Achievement;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserAchievement;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.AchievementRepository;
import com.languageschool.backend.repository.UserAchievementRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.UserAchievementService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UserAchievementServiceImpl implements UserAchievementService {

    private final UserAchievementRepository repo;
    private final UserRepository userRepo;
    private final AchievementRepository achievementRepo;

    public UserAchievementServiceImpl(UserAchievementRepository repo,
                                      UserRepository userRepo,
                                      AchievementRepository achievementRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.achievementRepo = achievementRepo;
    }

    @Override
    public List<UserAchievementDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        return repo.findAll(Sort.by(Sort.Direction.DESC, "earnedAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public UserAchievementDto create(CreateUserAchievementRequest req) {
        User user = meUser();
        Achievement achievement = achievementRepo.findById(req.getAchievementId())
                .orElseThrow(ApiException::notFound);

        if (repo.findByUser_IdAndAchievement_Id(user.getId(), achievement.getId()).isPresent()) {
            throw ApiException.conflict(ErrorCode.ALREADY_EARNED);
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = SecurityUtils.isAdmin(auth);
        int required = achievement.getRequiredXp() == null ? 0 : achievement.getRequiredXp();
        int totalXp = user.getTotalXp() == null ? 0 : user.getTotalXp();
        if (!isAdmin && totalXp < required) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, ErrorCode.UNPROCESSABLE_ENTITY);
        }

        UserAchievement ua = UserAchievement.builder()
                .user(user)
                .achievement(achievement)
                .build();

        return toDto(repo.save(ua));
    }

    @Override
    public UserAchievementDto getById(Long id) {
        return getSecured(id);
    }

    @Override
    public Optional<UserAchievementDto> findByUserAndAchievement(Long userId, Long achievementId) {
        String ownerLogin = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(ownerLogin);
        return repo.findByUser_IdAndAchievement_Id(userId, achievementId)
                .map(this::toDto);
    }

    @Override
    public List<UserAchievementDto> findByUser(Long userId) {
        String ownerLogin = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(ownerLogin);
        return repo.findByUser_Id(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public List<UserAchievementDto> findByAchievement(Long achievementId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        return repo.findByAchievement_Id(achievementId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    @Override
    public void delete(Long id) {
        UserAchievement ua = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.requireAdmin(auth);
        repo.delete(ua);
    }

    @Override
    @Transactional(readOnly = true)
    public UserAchievementDto getSecured(Long id) {
        UserAchievement ua = repo.findById(id)
                .orElseThrow(ApiException::notFound);
        ensureOwnerOrAdmin(ua.getUser() != null ? ua.getUser().getLogin() : null);
        return toDto(ua);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAchievementDto> findByUserSecured(Long userId) {
        String ownerLogin = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(ownerLogin);
        return repo.findByUser_Id(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAchievementDto> getByUserAndAchievementSecured(Long userId, Long achievementId) {
        String ownerLogin = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(ownerLogin);
        return repo.findByUser_IdAndAchievement_Id(userId, achievementId)
                .map(this::toDto);
    }

    private User meUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        return userRepo.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }

    private UserAchievementDto toDto(UserAchievement ua) {
        return UserAchievementDto.builder()
                .id(ua.getId())
                .userId(ua.getUser() != null ? ua.getUser().getId() : null)
                .achievementId(ua.getAchievement() != null ? ua.getAchievement().getId() : null)
                .earnedAt(ua.getEarnedAt())
                .build();
    }
}
