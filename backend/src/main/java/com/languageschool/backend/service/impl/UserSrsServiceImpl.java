package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userSrs.UserSrsDto;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.UserSrs;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.ExerciseRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.repository.UserSrsRepository;
import com.languageschool.backend.service.UserSrsService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserSrsServiceImpl implements UserSrsService {

    private final UserSrsRepository repo;
    private final UserRepository userRepo;
    private final ExerciseRepository exerciseRepo;

    public UserSrsServiceImpl(UserSrsRepository repo,
                              UserRepository userRepo,
                              ExerciseRepository exerciseRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.exerciseRepo = exerciseRepo;
    }

    @Override
    public List<UserSrsDto> findByUser(Long userId) {
        String login = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(login);
        return repo.findByUser_IdOrderByDueAtAsc(userId)
                .stream().map(this::toDto).toList();
    }

    @Override
    public List<UserSrsDto> findDueByUser(Long userId, Instant dueBefore) {
        String login = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(login);
        return repo.findByUser_IdAndDueAtBeforeOrderByDueAtAsc(userId, dueBefore)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    @Override
    public UserSrsDto review(Long userId, Long exerciseId, Integer quality) {
        String login = userRepo.findById(userId)
                .orElseThrow(ApiException::notFound)
                .getLogin();
        ensureOwnerOrAdmin(login);

        Exercise e = exerciseRepo.findById(exerciseId)
                .orElseThrow(ApiException::notFound);

        UserSrs s = repo.findByUser_IdAndExercise_Id(userId, exerciseId)
                .orElseGet(() -> {
                    UserSrs ns = new UserSrs();
                    ns.setUser(userRepo.findById(userId)
                            .orElseThrow(ApiException::notFound));
                    ns.setExercise(e);
                    ns.setIntervalDays(0);
                    ns.setRepetitions(0);
                    ns.setDueAt(Instant.now());
                    return ns;
                });

        int q = Math.max(0, Math.min(5, quality == null ? 0 : quality));
        int reps = s.getRepetitions() == null ? 0 : s.getRepetitions();
        int interval = s.getIntervalDays() == null ? 0 : s.getIntervalDays();

        if (q >= 4) {
            if (reps == 0) {
                interval = 1;
            } else if (reps == 1) {
                interval = 6;
            } else {
                interval = Math.max(1, interval * 2);
            }
            reps = reps + 1;
        } else {
            reps = 0;
            interval = 1;
        }

        s.setRepetitions(reps);
        s.setIntervalDays(interval);
        s.setLastQuality(q);
        s.setDueAt(Instant.now().plus(interval, ChronoUnit.DAYS));

        return toDto(repo.save(s));
    }

    private UserSrsDto toDto(UserSrs s) {
        return UserSrsDto.builder()
                .id(s.getId())
                .userId(s.getUser() != null ? s.getUser().getId() : null)
                .exerciseId(s.getExercise() != null ? s.getExercise().getId() : null)
                .dueAt(s.getDueAt())
                .intervalDays(s.getIntervalDays())
                .repetitions(s.getRepetitions())
                .lastQuality(s.getLastQuality())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private void ensureOwnerOrAdmin(String ownerLogin) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        SecurityUtils.ensureOwnerOrAdmin(auth, ownerLogin);
    }
}
