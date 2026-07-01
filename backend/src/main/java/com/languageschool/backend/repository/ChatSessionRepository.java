package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    List<ChatSession> findByUser_IdOrderByStartedAtDesc(Long userId);

}
