package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userSrs.SrsSummaryDto;
import com.languageschool.backend.service.SrsSummaryService;
import com.languageschool.backend.util.SecurityUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.springframework.stereotype.Service;

@Service
@Transactional
public class SrsSummaryServiceImpl implements SrsSummaryService {

    @PersistenceContext
    private EntityManager em;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    @Override
    public SrsSummaryDto getSummaryForLogin(String login) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, login);

        ZonedDateTime now = ZonedDateTime.now(ZONE);
        ZonedDateTime startToday = now.toLocalDate().atStartOfDay(ZONE);
        ZonedDateTime endToday = startToday.plusDays(1).minusNanos(1);
        ZonedDateTime end7 = startToday.plusDays(7).minusNanos(1);

        Long dueToday = em.createQuery(
                        "select count(us.id) from UserSrs us " +
                                "where us.deletedAt is null and us.user.login=:login and us.dueAt<=:endToday",
                        Long.class)
                .setParameter("login", login)
                .setParameter("endToday", endToday.toInstant())
                .getSingleResult();

        Long dueNext7 = em.createQuery(
                        "select count(us.id) from UserSrs us " +
                                "where us.deletedAt is null and us.user.login=:login and us.dueAt>:endToday and us.dueAt<=:end7",
                        Long.class)
                .setParameter("login", login)
                .setParameter("endToday", endToday.toInstant())
                .setParameter("end7", end7.toInstant())
                .getSingleResult();

        return SrsSummaryDto.builder()
                .dueTodayCount(dueToday == null ? 0 : dueToday)
                .dueNext7DaysCount(dueNext7 == null ? 0 : dueNext7)
                .build();
    }
}
