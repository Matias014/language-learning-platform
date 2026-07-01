package com.languageschool.backend.controller;

import com.languageschool.backend.dto.login.LoginRequest;
import com.languageschool.backend.dto.login.LoginResponse;
import com.languageschool.backend.dto.login.RefreshTokenResponse;
import com.languageschool.backend.dto.user.CreateUserRequest;
import com.languageschool.backend.dto.user.UserDto;
import com.languageschool.backend.entity.User;
import com.languageschool.backend.error.ApiException;
import com.languageschool.backend.repository.UserRepository;
import com.languageschool.backend.security.LoginThrottler;
import com.languageschool.backend.security.JwtService;
import com.languageschool.backend.service.RefreshTokenService;
import com.languageschool.backend.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;
    private final LoginThrottler throttler;
    private final RefreshTokenService refreshTokens;

    public AuthController(AuthenticationManager authManager,
                          JwtService jwtService,
                          UserService userService,
                          UserRepository userRepository,
                          LoginThrottler throttler,
                          RefreshTokenService refreshTokens) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
        this.throttler = throttler;
        this.refreshTokens = refreshTokens;
    }

    @PostMapping("/auth/register")
    public ResponseEntity<LoginResponse> register(@Valid @RequestBody CreateUserRequest req,
                                                  HttpServletRequest httpReq,
                                                  HttpServletResponse res) {
        UserDto user = userService.create(req);
        JwtService.TokenWithExp jwt = jwtService.generateAccessToken(
                user.getLogin(),
                user.getRole().name().toLowerCase(Locale.ROOT),
                null
        );
        String refreshToken = refreshTokens.createForUser(user.getId());
        setRefreshCookie(httpReq, res, refreshToken, jwtService.getRefreshExpDays());
        long expiresIn = Math.max(0L, Duration.between(Instant.now(), jwt.expiresAt()).getSeconds());
        LoginResponse response = LoginResponse.builder()
                .accessToken(jwt.token())
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
        URI location = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/api/users/me")
                .build()
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest body,
                                               HttpServletRequest req,
                                               HttpServletResponse res) {
        throttler.assertAllowed(req, body.getLoginOrEmail());
        Authentication authentication;
        try {
            authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(body.getLoginOrEmail(), body.getPassword())
            );
        } catch (AuthenticationException ex) {
            String ip = req.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = req.getRemoteAddr();
            }
            log.warn("Failed login attempt user={} ip={}", body.getLoginOrEmail(), ip);
            throw ex;
        }
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String username = authentication.getName();
        User user = userRepository.findByLogin(username).orElseThrow(ApiException::notFound);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        JwtService.TokenWithExp jwt = jwtService.generateAccessToken(
                user.getLogin(),
                user.getRole().name().toLowerCase(Locale.ROOT),
                null
        );
        String refreshToken = refreshTokens.createForUser(user.getId());
        setRefreshCookie(req, res, refreshToken, jwtService.getRefreshExpDays());
        long expiresIn = Math.max(0L, Duration.between(Instant.now(), jwt.expiresAt()).getSeconds());
        LoginResponse response = LoginResponse.builder()
                .accessToken(jwt.token())
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(HttpServletRequest req,
                                                        HttpServletResponse res) {
        String token = readRefreshCookie(req);
        if (token == null || token.isBlank()) {
            clearRefreshCookie(req, res);
            throw ApiException.unauthorized();
        }
        Long userId = refreshTokens.findUserIdByToken(token).orElse(null);
        if (userId == null) {
            clearRefreshCookie(req, res);
            throw ApiException.unauthorized();
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            refreshTokens.revoke(token);
            clearRefreshCookie(req, res);
            throw ApiException.unauthorized();
        }

        JwtService.TokenWithExp jwt = jwtService.generateAccessToken(
                user.getLogin(),
                user.getRole().name().toLowerCase(Locale.ROOT),
                null
        );

        String outRefresh = token;
        if (jwtService.isRefreshRotate()) {
            refreshTokens.revoke(token);
            outRefresh = refreshTokens.createForUser(userId);
        }

        setRefreshCookie(req, res, outRefresh, jwtService.getRefreshExpDays());

        long expiresIn = Math.max(0L, Duration.between(Instant.now(), jwt.expiresAt()).getSeconds());
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken(jwt.token())
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest req,
                                       HttpServletResponse res) {
        String token = readRefreshCookie(req);
        if (token != null && !token.isBlank()) {
            refreshTokens.revoke(token);
        }
        clearRefreshCookie(req, res);
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    private void setRefreshCookie(HttpServletRequest req, HttpServletResponse res, String token, int expDays) {
        boolean secure = isSecureRequest(req);
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), token)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(expDays))
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearRefreshCookie(HttpServletRequest req, HttpServletResponse res) {
        boolean secure = isSecureRequest(req);
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private boolean isSecureRequest(HttpServletRequest req) {
        if (req.isSecure()) {
            return true;
        }
        String proto = req.getHeader("X-Forwarded-Proto");
        return proto != null && proto.equalsIgnoreCase("https");
    }

    private String readRefreshCookie(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (jwtService.getRefreshCookieName().equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }
}
