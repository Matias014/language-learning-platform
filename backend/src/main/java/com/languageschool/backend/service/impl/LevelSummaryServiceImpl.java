package com.languageschool.backend.service.impl;

import com.languageschool.backend.dto.userLevel.LevelSummaryDto;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserLevel;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.UserLevelRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.LevelSummaryService;
import com.languageschool.backend.util.SecurityUtils;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class LevelSummaryServiceImpl implements LevelSummaryService {

    private final UserRepository userRepo;
    private final UserLevelRepository levelRepo;

    public LevelSummaryServiceImpl(UserRepository userRepo, UserLevelRepository levelRepo) {
        this.userRepo = userRepo;
        this.levelRepo = levelRepo;
    }

    @Override
    public LevelSummaryDto getSummaryForLogin(String login) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        SecurityUtils.ensureOwnerOrAdmin(auth, login);

        User user = userRepo.findByLogin(login).orElseThrow(ApiException::notFound);
        int totalXp = user.getTotalXp() == null ? 0 : user.getTotalXp();

        List<UserLevel> levels = levelRepo.findAll(Sort.by(Sort.Order.asc("requiredXp"), Sort.Order.asc("level")));
        if (levels.isEmpty()) {
            return LevelSummaryDto.builder().currentLevel(1).percentToNext(0).build();
        }

        int idx = -1;
        int base = 0;
        for (int i = 0; i < levels.size(); i++) {
            int req = nvl(levels.get(i).getRequiredXp());
            if (req <= totalXp) {
                idx = i;
                base = req;
            } else {
                break;
            }
        }

        Integer currentLevel = 1;
        if (idx >= 0) {
            Integer lv = levels.get(idx).getLevel();
            currentLevel = lv == null ? 1 : lv;
        }

        UserLevel next = (idx + 1 < levels.size()) ? levels.get(idx + 1) : null;
        int percent;
        if (next == null) {
            percent = 100;
        } else {
            int target = nvl(next.getRequiredXp());
            int range = Math.max(1, target - base);
            int progress = Math.max(0, totalXp - base);
            percent = clamp(Math.round(progress * 100.0f / range));
        }

        return LevelSummaryDto.builder()
                .currentLevel(currentLevel)
                .percentToNext(percent)
                .build();
    }

    private int nvl(Integer v) {
        return Objects.requireNonNullElse(v, 0);
    }

    private int clamp(int p) {
        if (p < 0) return 0;
        if (p > 100) return 100;
        return p;
    }
}
