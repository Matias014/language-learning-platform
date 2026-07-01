package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.xp.XpBreakdownDto;
import com.languageschool.backend.entity.ExerciseAward;
import com.languageschool.backend.entity.Exercise;
import com.languageschool.backend.entity.ExerciseAttempt;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.ExerciseAwardRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.XpAnalyticsService;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class XpAnalyticsServiceImpl implements XpAnalyticsService {

    private final ExerciseAwardRepository awardRepo;
    private final UserRepository userRepo;

    public XpAnalyticsServiceImpl(ExerciseAwardRepository awardRepo, UserRepository userRepo) {
        this.awardRepo = awardRepo;
        this.userRepo = userRepo;
    }

    @Override
    public XpBreakdownDto getMyBreakdown(int days) {
        int window = normalizeDays(days);
        User me = currentUser();
        Instant end = Instant.now();
        Instant start = end.minusSeconds(window * 24L * 60 * 60);

        List<ExerciseAward> awards = awardRepo.findByAttempt_User_IdAndAwardedAtBetween(me.getId(), start, end);

        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byDifficulty = new HashMap<>();

        for (ExerciseAward a : awards) {
            Integer xp = a.getAwardedXp() == null ? 0 : a.getAwardedXp();
            ExerciseAttempt att = a.getAttempt();
            if (att == null) {
                continue;
            }
            Exercise ex = att.getExercise();
            if (ex == null) {
                continue;
            }

            String type = ex.getType() == null ? "unknown" : ex.getType().name();
            String diff = ex.getDifficulty() == null ? "unknown" : ex.getDifficulty().name();

            byType.merge(type, xp, Integer::sum);
            byDifficulty.merge(diff, xp, Integer::sum);
        }

        return XpBreakdownDto.builder()
                .days(window)
                .byType(byType)
                .byDifficulty(byDifficulty)
                .build();
    }

    private int normalizeDays(int d) {
        if (d == 7 || d == 30 || d == 90) {
            return d;
        }
        return 7;
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new InsufficientAuthenticationException("UNAUTHENTICATED");
        }
        return userRepo.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound);
    }
}
