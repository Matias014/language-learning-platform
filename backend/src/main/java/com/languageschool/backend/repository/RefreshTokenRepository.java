package com.languageschool.backend.repository;

import com.languageschool.backend.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    long deleteByUser_Id(Long userId);

    long deleteByExpiresAtBefore(Instant threshold);
}
