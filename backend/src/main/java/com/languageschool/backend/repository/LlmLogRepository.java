package com.languageschool.backend.repository;

import com.languageschool.backend.entity.LlmLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface LlmLogRepository extends JpaRepository<LlmLog, Long> {

    List<LlmLog> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<LlmLog> findByLesson_IdOrderByCreatedAtDesc(Long lessonId);

    List<LlmLog> findByExerciseAttempt_IdOrderByCreatedAtDesc(Long attemptId);

    List<LlmLog> findByChatSession_IdOrderByCreatedAtDesc(Long sessionId);

    List<LlmLog> findByChatSession_User_IdOrderByCreatedAtDesc(Long userId);

    void deleteByChatSession_Id(Long sessionId);

    void deleteByCreatedAtBefore(Instant threshold);
}
