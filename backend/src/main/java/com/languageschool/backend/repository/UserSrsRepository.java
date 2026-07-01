package com.languageschool.backend.repository;

import com.languageschool.backend.entity.UserSrs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserSrsRepository extends JpaRepository<UserSrs, Long> {

    List<UserSrs> findByUser_IdOrderByDueAtAsc(Long userId);

    List<UserSrs> findByUser_IdAndDueAtBeforeOrderByDueAtAsc(Long userId, Instant dueAt);

    Optional<UserSrs> findByUser_IdAndExercise_Id(Long userId, Long exerciseId);

    List<UserSrs> findByUser_IdAndDueAtBetween(Long userId, Instant start, Instant end);
}
