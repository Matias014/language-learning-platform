package com.languageschool.backend.repository;

import com.languageschool.backend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {

    Optional<UserAchievement> findByUser_IdAndAchievement_Id(Long userId, Long achievementId);

    List<UserAchievement> findByUser_Id(Long userId);

    List<UserAchievement> findByAchievement_Id(Long achievementId);
}
