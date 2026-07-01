package com.languageschool.backend.service.impl;

import com.languageschool.backend.entity.Achievement;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.entity.UserAchievement;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.AchievementRepository;
import com.languageschool.backend.repository.UserAchievementRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.AchievementAutoAwardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AchievementAutoAwardServiceImpl implements AchievementAutoAwardService {

    private final AchievementRepository achievementRepo;
    private final UserAchievementRepository userAchievementRepo;
    private final UserRepository userRepo;

    public AchievementAutoAwardServiceImpl(AchievementRepository achievementRepo,
                                           UserAchievementRepository userAchievementRepo,
                                           UserRepository userRepo) {
        this.achievementRepo = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    @Override
    public void awardMissingForUserId(Long userId) {
        User user = userRepo.findById(userId).orElseThrow(ApiException::notFound);
        int totalXp = user.getTotalXp() == null ? 0 : user.getTotalXp();

        List<Achievement> all = achievementRepo.findAll();
        if (all.isEmpty()) {
            return;
        }

        Set<Long> already = new HashSet<>();
        userAchievementRepo.findByUser_Id(user.getId()).forEach(ua -> {
            if (ua.getAchievement() != null) {
                already.add(ua.getAchievement().getId());
            }
        });

        for (Achievement a : all) {
            Integer req = a.getRequiredXp();
            if (req == null) {
                req = 0;
            }
            if (totalXp >= req && !already.contains(a.getId())) {
                UserAchievement ua = UserAchievement.builder()
                        .user(user)
                        .achievement(a)
                        .build();
                userAchievementRepo.save(ua);
                already.add(a.getId());
            }
        }
    }
}
