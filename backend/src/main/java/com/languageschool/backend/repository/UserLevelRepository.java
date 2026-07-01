package com.languageschool.backend.repository;

import com.languageschool.backend.entity.UserLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserLevelRepository extends JpaRepository<UserLevel, Integer> {

    List<UserLevel> findByRequiredXpLessThanEqualOrderByLevelDesc(int xp);
}
