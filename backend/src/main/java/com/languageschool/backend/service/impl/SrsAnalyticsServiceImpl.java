package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userSrs.DayCountDto;
import com.languageschool.backend.dto.userSrs.SrsThisWeekDto;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserSrs;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.repository.UserSrsRepository;
import com.languageschool.backend.service.SrsAnalyticsService;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class SrsAnalyticsServiceImpl implements SrsAnalyticsService {

    private final UserSrsRepository srsRepo;
    private final UserRepository userRepo;

    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");

    public SrsAnalyticsServiceImpl(UserSrsRepository srsRepo, UserRepository userRepo) {
        this.srsRepo = srsRepo;
        this.userRepo = userRepo;
    }

    @Override
    public SrsThisWeekDto getMyThisWeek() {
        User me = currentUser();
        ZoneId zone = ZONE;
        LocalDate now = LocalDate.now(zone);
        LocalDate monday = now.with(DayOfWeek.MONDAY);
        LocalDate sunday = monday.plusDays(6);

        Instant start = monday.atStartOfDay(zone).toInstant();
        Instant end = sunday.atTime(LocalTime.MAX).atZone(zone).toInstant();

        List<UserSrs> list = srsRepo.findByUser_IdAndDueAtBetween(me.getId(), start, end);

        Map<LocalDate, Integer> perDay = new HashMap<>();
        LocalDate d = monday;
        while (!d.isAfter(sunday)) {
            perDay.put(d, 0);
            d = d.plusDays(1);
        }

        for (UserSrs u : list) {
            Instant due = u.getDueAt();
            if (due == null) {
                continue;
            }
            LocalDate day = due.atZone(zone).toLocalDate();
            if (day.isBefore(monday) || day.isAfter(sunday)) {
                continue;
            }
            perDay.merge(day, 1, Integer::sum);
        }

        List<DayCountDto> byDay = new ArrayList<>();
        d = monday;
        int total = 0;
        while (!d.isAfter(sunday)) {
            int c = perDay.getOrDefault(d, 0);
            byDay.add(DayCountDto.builder().date(d.toString()).count(c).build());
            total += c;
            d = d.plusDays(1);
        }

        return SrsThisWeekDto.builder()
                .dueTotal(total)
                .byDay(byDay)
                .build();
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
