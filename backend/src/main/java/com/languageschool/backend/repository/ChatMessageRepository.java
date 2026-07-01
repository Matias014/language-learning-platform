package com.languageschool.backend.repository;

import com.languageschool.backend.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findBySession_IdOrderBySentAtAsc(Long sessionId);
}
