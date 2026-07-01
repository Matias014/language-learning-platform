package com.languageschool.backend.security;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder encoder;
    private final String issuer;
    private final long accessExpMin;
    @Getter
    private final int refreshExpDays;
    @Getter
    private final String refreshCookieName;
    @Getter
    private final boolean refreshRotate;

    public JwtService(
            JwtEncoder encoder,
            @Value("${security.jwt.issuer}") String issuer,
            @Value("${security.jwt.access-exp-min}") long accessExpMin,
            @Value("${security.jwt.refresh-exp-days}") int refreshExpDays,
            @Value("${security.jwt.refresh-cookie-name}") String refreshCookieName,
            @Value("${security.jwt.refresh-rotate}") boolean refreshRotate
    ) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.accessExpMin = accessExpMin;
        this.refreshExpDays = refreshExpDays;
        this.refreshCookieName = refreshCookieName;
        this.refreshRotate = refreshRotate;
    }

    public record TokenWithExp(String token, Instant expiresAt) {
    }

    public TokenWithExp generateAccessToken(String subject, String role, Map<String, Object> extraClaims) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessExpMin, ChronoUnit.MINUTES);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(subject)
                .issuedAt(now)
                .expiresAt(exp)
                .claim("role", role);
        if (extraClaims != null && !extraClaims.isEmpty()) {
            extraClaims.forEach(claims::claim);
        }
        JwsHeader jws = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(jws, claims.build())).getTokenValue();
        return new TokenWithExp(token, exp);
    }
}
