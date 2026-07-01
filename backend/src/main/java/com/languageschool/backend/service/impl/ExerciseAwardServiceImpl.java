package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.exerciseAward.CreateExerciseAwardRequest;
import com.languageschool.backend.dto.exerciseAward.ExerciseAwardDto;
import com.languageschool.backend.entity.ExerciseAttempt;
import com.languageschool.backend.entity.ExerciseAward;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserAchievement;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.repository.ExerciseAttemptRepository;
import com.languageschool.backend.repository.ExerciseAwardRepository;
import com.languageschool.backend.repository.UserAchievementRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.AchievementAutoAwardService;
import com.languageschool.backend.service.CourseProgressService;
import com.languageschool.backend.service.ExerciseAwardService;
import com.languageschool.backend.service.UserService;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.languageschool.backend.util.SecurityUtils.ensureOwnerOrAdmin;
import static com.languageschool.backend.util.SecurityUtils.isAdmin;

@Service
public class ExerciseAwardServiceImpl implements ExerciseAwardService {

    private final ExerciseAwardRepository repo;
    private final ExerciseAttemptRepository attemptRepo;
    private final UserRepository userRepo;
    private final CourseProgressService courseProgress;
    private final UserService userService;
    private final AchievementAutoAwardService achievementAutoAwardService;
    private final UserAchievementRepository userAchievementRepo;

    public ExerciseAwardServiceImpl(ExerciseAwardRepository repo,
                                    ExerciseAttemptRepository attemptRepo,
                                    UserRepository userRepo,
                                    CourseProgressService courseProgress,
                                    UserService userService,
                                    AchievementAutoAwardService achievementAutoAwardService,
                                    UserAchievementRepository userAchievementRepo) {
        this.repo = repo;
        this.attemptRepo = attemptRepo;
        this.userRepo = userRepo;
        this.courseProgress = courseProgress;
        this.userService = userService;
        this.achievementAutoAwardService = achievementAutoAwardService;
        this.userAchievementRepo = userAchievementRepo;
    }

    @Override
    public List<ExerciseAwardDto> findAll() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }
        return repo.findAll(Sort.by(Sort.Direction.DESC, "awardedAt")).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public ExerciseAwardDto create(CreateExerciseAwardRequest req) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ExerciseAttempt a = attemptRepo.findById(req.getAttemptId()).orElseThrow(ApiException::notFound);
        Long ownerId = a.getUser() != null ? a.getUser().getId() : null;
        ensureOwnerOrAdmin(userService, auth, ownerId);

        if (!a.isCorrect()) {
            throw ApiException.badRequest(ErrorCode.ATTEMPT_NOT_MARKED_CORRECT);
        }
        if (repo.findByAttempt_Id(a.getId()).isPresent()) {
            throw ApiException.conflict(ErrorCode.AWARD_ALREADY_EXISTS_FOR_ATTEMPT);
        }
        if (repo.findTopByAttempt_User_IdAndAttempt_Exercise_IdOrderByAwardedAtDesc(
                a.getUser().getId(), a.getExercise().getId()).isPresent()) {
            throw ApiException.conflict(ErrorCode.ALREADY_EXISTS);
        }

        int xp = a.getExercise().getXp() != null ? a.getExercise().getXp() : 0;

        ExerciseAward award = new ExerciseAward();
        award.setAttempt(a);
        award.setAwardedXp(xp);

        ExerciseAward saved = repo.save(award);

        User u = a.getUser();
        int currentXp = u.getTotalXp() == null ? 0 : u.getTotalXp();
        u.setTotalXp(Math.max(0, currentXp + saved.getAwardedXp()));
        userRepo.save(u);

        courseProgress.recalcForUserCourse(u.getId(), a.getExercise().getLesson().getCourse().getId());
        achievementAutoAwardService.awardMissingForUserId(u.getId());

        return toDto(saved);
    }

    @Transactional(propagation = Propagation.REQUIRED)
    @Override
    public Optional<ExerciseAwardDto> createIfEligible(Long attemptId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        ExerciseAttempt a = attemptRepo.findById(attemptId).orElseThrow(ApiException::notFound);
        Long ownerId = a.getUser() != null ? a.getUser().getId() : null;
        ensureOwnerOrAdmin(userService, auth, ownerId);

        if (!a.isCorrect()) {
            throw ApiException.badRequest(ErrorCode.ATTEMPT_NOT_MARKED_CORRECT);
        }

        Optional<ExerciseAward> byAttempt = repo.findByAttempt_Id(a.getId());
        if (byAttempt.isPresent()) {
            return byAttempt.map(this::toDto);
        }

        boolean alreadyAwardedForExercise = repo
                .findTopByAttempt_User_IdAndAttempt_Exercise_IdOrderByAwardedAtDesc(
                        a.getUser().getId(), a.getExercise().getId())
                .isPresent();
        if (alreadyAwardedForExercise) {
            return Optional.empty();
        }

        int xp = a.getExercise().getXp() != null ? a.getExercise().getXp() : 0;

        ExerciseAward award = new ExerciseAward();
        award.setAttempt(a);
        award.setAwardedXp(xp);

        ExerciseAward saved = repo.save(award);

        User u = a.getUser();
        int currentXp = u.getTotalXp() == null ? 0 : u.getTotalXp();
        u.setTotalXp(Math.max(0, currentXp + saved.getAwardedXp()));
        userRepo.save(u);

        courseProgress.recalcForUserCourse(u.getId(), a.getExercise().getLesson().getCourse().getId());
        achievementAutoAwardService.awardMissingForUserId(u.getId());

        return Optional.of(toDto(saved));
    }

    @Override
    public Optional<ExerciseAwardDto> findById(Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }
        return repo.findById(id).map(this::toDto);
    }

    @Override
    public Optional<ExerciseAwardDto> findByUserAndExercise(Long userId, Long exerciseId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, userId);
        return repo.findTopByAttempt_User_IdAndAttempt_Exercise_IdOrderByAwardedAtDesc(userId, exerciseId)
                .map(this::toDto);
    }

    @Override
    public Optional<ExerciseAwardDto> findByAttempt(Long attemptId) {
        return attemptRepo.findById(attemptId).flatMap(attempt -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            Long ownerId = attempt.getUser() != null ? attempt.getUser().getId() : null;
            if (ownerId != null) {
                ensureOwnerOrAdmin(userService, auth, ownerId);
            } else {
                if (!isAdmin(auth)) {
                    throw ApiException.forbidden();
                }
            }
            return repo.findByAttempt_Id(attemptId).map(this::toDto);
        });
    }

    @Override
    public List<ExerciseAwardDto> findByUser(Long userId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        ensureOwnerOrAdmin(userService, auth, userId);
        return repo.findByAttempt_User_Id(userId).stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public void delete(Long id) {
        ExerciseAward award = repo.findById(id).orElseThrow(ApiException::notFound);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAdmin(auth)) {
            throw ApiException.forbidden();
        }

        User u = award.getAttempt().getUser();
        Long courseId = award.getAttempt().getExercise().getLesson().getCourse().getId();

        int currentXp = u.getTotalXp() == null ? 0 : u.getTotalXp();
        u.setTotalXp(Math.max(0, currentXp - award.getAwardedXp()));
        userRepo.save(u);

        repo.delete(award);
        courseProgress.recalcForUserCourse(u.getId(), courseId);
        revokeUnmetAchievements(u);
    }

    private void revokeUnmetAchievements(User u) {
        int totalXp = u.getTotalXp() == null ? 0 : u.getTotalXp();
        List<UserAchievement> list = userAchievementRepo.findByUser_Id(u.getId());
        for (UserAchievement ua : list) {
            if (ua.getAchievement() == null) {
                continue;
            }
            Integer req = ua.getAchievement().getRequiredXp();
            int r = req == null ? 0 : req;
            if (totalXp < r) {
                userAchievementRepo.delete(ua);
            }
        }
    }

    private ExerciseAwardDto toDto(ExerciseAward a) {
        return ExerciseAwardDto.builder()
                .id(a.getId())
                .attemptId(a.getAttempt().getId())
                .awardedXp(a.getAwardedXp())
                .awardedAt(a.getAwardedAt())
                .build();
    }
}
