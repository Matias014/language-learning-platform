package com.languageschool.backend.service;

import java.util.Optional;

public interface RefreshTokenService {
    String createForUser(Long userId);

    Optional<Long> findUserIdByToken(String token);

    void revoke(String token);

    long revokeAllForUser(Long userId);

    long purgeExpired();
}
