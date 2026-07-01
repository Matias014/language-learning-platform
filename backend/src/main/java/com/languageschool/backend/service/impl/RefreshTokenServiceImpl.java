package com.languageschool.backend.service.impl;

import com.languageschool.backend.entity.RefreshToken;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.RefreshTokenRepository;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.service.RefreshTokenService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Optional;

@Service
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository repo;
    private final UserRepository userRepo;
    private final int ttlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenServiceImpl(RefreshTokenRepository repo,
                                   UserRepository userRepo,
                                   @Value("${security.jwt.refresh-exp-days:30}") int ttlDays) {
        this.repo = repo;
        this.userRepo = userRepo;
        this.ttlDays = ttlDays;
    }

    @Override
    public String createForUser(Long userId) {
        User u = userRepo.findById(userId).orElseThrow(ApiException::notFound);
        repo.deleteByUser_Id(u.getId());
        String raw = generateToken();
        String hash = sha256(raw);
        Instant now = Instant.now();
        RefreshToken rt = new RefreshToken();
        rt.setUser(u);
        rt.setTokenHash(hash);
        rt.setIssuedAt(now);
        rt.setExpiresAt(now.plus(ttlDays, ChronoUnit.DAYS));
        repo.save(rt);
        return raw;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Long> findUserIdByToken(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        String hash = sha256(token);
        Optional<RefreshToken> opt = repo.findByTokenHash(hash);
        if (opt.isEmpty()) return Optional.empty();
        RefreshToken rt = opt.get();
        if (rt.getExpiresAt() != null && rt.getExpiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.ofNullable(rt.getUser()).map(User::getId);
    }

    @Override
    public void revoke(String token) {
        if (token == null || token.isBlank()) return;
        String hash = sha256(token);
        repo.findByTokenHash(hash).ifPresent(repo::delete);
    }

    @Override
    public long revokeAllForUser(Long userId) {
        return repo.deleteByUser_Id(userId);
    }

    @Override
    public long purgeExpired() {
        return repo.deleteByExpiresAtBefore(Instant.now());
    }

    private String generateToken() {
        byte[] buf = new byte[48];
        secureRandom.nextBytes(buf);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(dig);
        } catch (Exception e) {
            throw new IllegalStateException("HASH_ERROR", e);
        }
    }
}
