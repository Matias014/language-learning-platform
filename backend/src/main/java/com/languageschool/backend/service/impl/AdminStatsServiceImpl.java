package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.adminStats.HardestExerciseDto;
import com.languageschool.backend.dto.adminStats.LlmStatsDto;
import com.languageschool.backend.entity.InteractionType;
import com.languageschool.backend.entity.LlmStatus;
import com.languageschool.backend.service.AdminStatsService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminStatsServiceImpl implements AdminStatsService {

    @PersistenceContext
    private EntityManager em;

    private Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Override
    public LlmStatsDto getLlmStats() {
        SecurityUtils.requireAdmin(auth());

        Object[] agg = em.createQuery(
                        "select count(l.id), coalesce(sum(l.tokensIn),0), coalesce(sum(l.tokensOut),0), avg(l.latencyMs) " +
                                "from LlmLog l",
                        Object[].class)
                .getSingleResult();

        Long calls = ((Number) agg[0]).longValue();
        Long tokensIn = ((Number) agg[1]).longValue();
        Long tokensOut = ((Number) agg[2]).longValue();
        BigDecimal avgLatency = agg[3] == null
                ? null
                : BigDecimal.valueOf(((Number) agg[3]).doubleValue()).setScale(2, RoundingMode.HALF_UP);

        List<Object[]> byType = em.createQuery(
                        "select l.interactionType, count(l.id) from LlmLog l group by l.interactionType",
                        Object[].class)
                .getResultList();
        Map<InteractionType, Long> byTypeMap = new EnumMap<>(InteractionType.class);
        for (Object[] r : byType) {
            byTypeMap.put((InteractionType) r[0], ((Number) r[1]).longValue());
        }

        List<Object[]> byStatus = em.createQuery(
                        "select l.status, count(l.id) from LlmLog l group by l.status",
                        Object[].class)
                .getResultList();
        Map<LlmStatus, Long> byStatusMap = new EnumMap<>(LlmStatus.class);
        for (Object[] r : byStatus) {
            byStatusMap.put((LlmStatus) r[0], ((Number) r[1]).longValue());
        }

        return LlmStatsDto.builder()
                .calls(calls)
                .tokensIn(tokensIn)
                .tokensOut(tokensOut)
                .averageLatencyMs(avgLatency)
                .callsByInteractionType(byTypeMap)
                .callsByStatus(byStatusMap)
                .build();
    }

    @Override
    public List<HardestExerciseDto> getHardestExercises(int limit) {
        SecurityUtils.requireAdmin(auth());

        List<Object[]> rows = em.createQuery(
                        "select ea.exercise.id, avg(case when ea.correct=true then 1.0 else 0.0 end), avg(ea.durationSeconds) " +
                                "from ExerciseAttempt ea " +
                                "where ea.deletedAt is null " +
                                "group by ea.exercise.id " +
                                "order by avg(case when ea.correct=true then 1.0 else 0.0 end) asc",
                        Object[].class)
                .setMaxResults(Math.max(1, Math.min(limit, 100)))
                .getResultList();

        return rows.stream()
                .map(r -> HardestExerciseDto.builder()
                        .exerciseId((Long) r[0])
                        .averageAccuracy(r[1] == null
                                ? null
                                : BigDecimal.valueOf(((Number) r[1]).doubleValue()).setScale(4, RoundingMode.HALF_UP))
                        .averageDurationSeconds(r[2] == null ? null : ((Number) r[2]).intValue())
                        .build())
                .toList();
    }
}
