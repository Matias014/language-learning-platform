package com.languageschool.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final Environment env;
    private final ObjectMapper objectMapper;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String corsAllowedOrigins;

    public SecurityConfig(Environment env, ObjectMapper objectMapper) {
        this.env = env;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthenticationConverter jwtAuthConverter) throws Exception {
        List<String> profiles = Arrays.asList(env.getActiveProfiles());
        boolean isDev = profiles.contains("dev");
        boolean isTest = profiles.contains("test");
        boolean isProd = profiles.contains("prod");
        boolean sslEnabled = env.getProperty("server.ssl.enabled", Boolean.class, false);

        http.csrf(csrf -> csrf.disable());
        http.cors(Customizer.withDefaults());
        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.headers(headers -> {
            headers.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
            headers.contentTypeOptions(Customizer.withDefaults());
            headers.frameOptions(f -> f.deny());
            if (isProd && sslEnabled) {
                headers.httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(Duration.ofDays(365).getSeconds()));
            }
        });
        http.exceptionHandling(ex -> ex
                .authenticationEntryPoint((req, res, e) -> {
                    ProblemDetail pd = buildSecurityProblem(req, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setContentType("application/problem+json");
                    res.addHeader("WWW-Authenticate", "Bearer");
                    objectMapper.writeValue(res.getOutputStream(), pd);
                })
                .accessDeniedHandler((req, res, e) -> {
                    ProblemDetail pd = buildSecurityProblem(req, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setContentType("application/problem+json");
                    objectMapper.writeValue(res.getOutputStream(), pd);
                })
        );
        http.authorizeHttpRequests(auth -> {
            auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();
            auth.requestMatchers("/actuator/health", "/actuator/info").permitAll();
            auth.requestMatchers("/api/auth/**").permitAll();
            if (isDev || isTest) {
                auth.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll();
            }
            auth.requestMatchers(HttpMethod.GET,
                    "/api/courses", "/api/courses/**",
                    "/api/languages", "/api/languages/**",
                    "/api/proficiency-levels", "/api/proficiency-levels/**",
                    "/api/levels", "/api/levels/**"
            ).permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/media/**").permitAll();
            auth.requestMatchers(HttpMethod.HEAD, "/api/media/**").permitAll();
            auth.anyRequest().authenticated();
        });
        http.oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(jwtAuthConverter)));
        return http.build();
    }

    private ProblemDetail buildSecurityProblem(HttpServletRequest req, HttpStatus status, ErrorCode code) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setTitle(status.getReasonPhrase());
        pd.setDetail(code.name());
        pd.setProperty("code", code.name());
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("method", req.getMethod());
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "Origin", "Cache-Control", "Pragma", "X-Requested-With"));
        cfg.setExposedHeaders(List.of("Location"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null || role.isBlank()) {
                return List.of();
            }
            String authority = "ROLE_" + role.toUpperCase();
            return List.of(new SimpleGrantedAuthority(authority));
        });
        return converter;
    }
}
