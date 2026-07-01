package com.languageschool.backend.controller;

import com.languageschool.backend.dto.exerciseAward.ExerciseAwardDto;
import com.languageschool.backend.dto.xp.XpPointDto;
import com.languageschool.backend.dto.xp.XpSummaryDto;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.error.ErrorCode;
import com.languageschool.backend.service.ExerciseAwardService;
import com.languageschool.backend.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class XpController {

    private final ExerciseAwardService awards;
    private final UserService users;
    private static final ZoneId ZONE_ID = ZoneId.of("Europe/Warsaw");

    public XpController(ExerciseAwardService awards, UserService users) {
        this.awards = awards;
        this.users = users;
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/xp/summary")
    public XpSummaryDto meSummary(Authentication auth) {
        Long me = users.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return summaryForUser(me);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/xp/summary")
    public XpSummaryDto userSummary(@PathVariable Long userId) {
        return summaryForUser(userId);
    }

    private XpSummaryDto summaryForUser(Long userId) {
        List<ExerciseAwardDto> list = awards.findByUser(userId);
        long totalXp = list.stream()
                .mapToLong(a -> Optional.ofNullable(a.getAwardedXp()).orElse(0))
                .sum();
        return new XpSummaryDto(totalXp, list.size());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/users/me/xp/timeseries")
    public List<XpPointDto> meTimeseries(@RequestParam(name = "days", defaultValue = "7") int days,
                                         Authentication auth) {
        validateDays(days);
        Long me = users.findByLogin(auth.getName())
                .orElseThrow(ApiException::notFound)
                .getId();
        return timeseriesForUser(me, days);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/{userId}/xp/timeseries")
    public List<XpPointDto> userTimeseries(@PathVariable Long userId,
                                           @RequestParam(name = "days", defaultValue = "7") int days) {
        validateDays(days);
        return timeseriesForUser(userId, days);
    }

    private void validateDays(int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw ApiException.badRequest(ErrorCode.INVALID_DAYS);
        }
    }

    private List<XpPointDto> timeseriesForUser(Long userId, int days) {
        List<ExerciseAwardDto> list = awards.findByUser(userId);
        LocalDate today = LocalDate.now(ZONE_ID);
        LocalDate start = today.minusDays(days - 1L);

        Map<LocalDate, Long> map = new LinkedHashMap<>();
        for (int i = 0; i < days; i++) {
            map.put(start.plusDays(i), 0L);
        }

        for (ExerciseAwardDto a : list) {
            if (a.getAwardedAt() == null || a.getAwardedXp() == null) {
                continue;
            }
            LocalDate date = a.getAwardedAt()
                    .atZone(ZONE_ID)
                    .toLocalDate();

            if (!date.isBefore(start) && !date.isAfter(today)) {
                map.computeIfPresent(date, (k, v) -> v + a.getAwardedXp());
            }
        }

        return map.entrySet().stream()
                .map(e -> new XpPointDto(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }
}
