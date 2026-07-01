package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.stats.ActivityStatsDto;
import com.languageschool.backend.dto.stats.EffectivenessStatsDto;
import com.languageschool.backend.entity.DifficultyLevel;
import com.languageschool.backend.entity.ExerciseType;
import com.languageschool.backend.service.UserStatsService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserStatsServiceImpl implements UserStatsService {

    @PersistenceContext
    private EntityManager em;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Override
    public EffectivenessStatsDto getEffectivenessForLogin(String login) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, login);

        Long total = em.createQuery(
                        "select count(ea.id) from ExerciseAttempt ea where ea.deletedAt is null and ea.user.login=:login",
                        Long.class)
                .setParameter("login", login)
                .getSingleResult();
        Long correct = em.createQuery(
                        "select count(ea.id) from ExerciseAttempt ea where ea.deletedAt is null and ea.user.login=:login and ea.correct=true",
                        Long.class)
                .setParameter("login", login)
                .getSingleResult();
        BigDecimal global = total != 0 ? bd(correct).divide(bd(total), 4, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        List<Object[]> byTypeRows = em.createQuery(
                        "select e.type, avg(case when ea.correct=true then 1.0 else 0.0 end) " +
                                "from ExerciseAttempt ea join ea.exercise e " +
                                "where ea.deletedAt is null and ea.user.login=:login " +
                                "group by e.type",
                        Object[].class)
                .setParameter("login", login)
                .getResultList();
        Map<ExerciseType, BigDecimal> byType = new EnumMap<>(ExerciseType.class);
        for (Object[] r : byTypeRows) {
            byType.put((ExerciseType) r[0], toBd(r[1]));
        }

        List<Object[]> byDiffRows = em.createQuery(
                        "select e.difficulty, avg(case when ea.correct=true then 1.0 else 0.0 end) " +
                                "from ExerciseAttempt ea join ea.exercise e " +
                                "where ea.deletedAt is null and ea.user.login=:login " +
                                "group by e.difficulty",
                        Object[].class)
                .setParameter("login", login)
                .getResultList();
        Map<DifficultyLevel, BigDecimal> byDifficulty = new EnumMap<>(DifficultyLevel.class);
        for (Object[] r : byDiffRows) {
            byDifficulty.put((DifficultyLevel) r[0], toBd(r[1]));
        }

        Double avgScore = em.createQuery(
                        "select avg(ea.score) from ExerciseAttempt ea " +
                                "where ea.deletedAt is null and ea.user.login=:login and ea.score is not null",
                        Double.class)
                .setParameter("login", login)
                .getSingleResult();
        BigDecimal avgOpen = avgScore == null ? null : BigDecimal.valueOf(avgScore).setScale(4, RoundingMode.HALF_UP);

        return EffectivenessStatsDto.builder()
                .globalAccuracy(global)
                .accuracyByType(byType)
                .accuracyByDifficulty(byDifficulty)
                .averageOpenScore(avgOpen)
                .build();
    }

    @Override
    public ActivityStatsDto getActivityForLogin(String login) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, login);

        List<Instant> instants = em.createQuery(
                        "select ea.submittedAt from ExerciseAttempt ea " +
                                "where ea.deletedAt is null and ea.user.login=:login",
                        Instant.class)
                .setParameter("login", login)
                .getResultList();
        Instant last = instants.stream().max(Instant::compareTo).orElse(null);
        Set<LocalDate> days = new HashSet<>();
        for (Instant i : instants) {
            days.add(LocalDate.ofInstant(i, ZONE));
        }
        int activeDays = days.size();
        int streak = 0;
        LocalDate today = LocalDate.now(ZONE);
        LocalDate cursor = today;
        while (days.contains(cursor)) {
            streak += 1;
            cursor = cursor.minusDays(1);
        }
        return ActivityStatsDto.builder()
                .streakDays(streak)
                .activeDaysCount(activeDays)
                .lastActivityAt(last)
                .build();
    }

    private static BigDecimal bd(long v) {
        return BigDecimal.valueOf(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return null;
        return BigDecimal.valueOf(((Number) v).doubleValue()).setScale(4, RoundingMode.HALF_UP);
    }
}
