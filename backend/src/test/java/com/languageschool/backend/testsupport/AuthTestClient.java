package com.languageschool.backend.testsupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.dto.login.LoginRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestClient {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    public AuthTestClient(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    public AuthSession login(String loginOrEmail, String password) throws Exception {
        LoginRequest req = new LoginRequest();
        req.setLoginOrEmail(loginOrEmail);
        req.setPassword(password);

        MvcResult res = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(req))
                )
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = res.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsByteArray());
        String accessToken = body.path("accessToken").asText();

        Cookie refreshCookie = cookieFromSetCookies(setCookies, "refresh_token");
        return new AuthSession(accessToken, refreshCookie, setCookies.isEmpty() ? null : setCookies.get(0));
    }

    public AuthSession register(String login, String email, String password) throws Exception {
        String json = """
                {
                  "login": "%s",
                  "email": "%s",
                  "password": "%s",
                  "name": "Test",
                  "surname": "User"
                }
                """.formatted(login, email, password);

        MvcResult res = mockMvc.perform(
                        post("/api/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json)
                )
                .andExpect(status().isCreated())
                .andReturn();

        List<String> setCookies = res.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsByteArray());
        String accessToken = body.path("accessToken").asText();

        Cookie refreshCookie = cookieFromSetCookies(setCookies, "refresh_token");
        return new AuthSession(accessToken, refreshCookie, setCookies.isEmpty() ? null : setCookies.get(0));
    }

    public AuthSession refresh(Cookie refreshCookie) throws Exception {
        MvcResult res = mockMvc.perform(
                        post("/api/auth/refresh")
                                .cookie(refreshCookie)
                )
                .andExpect(status().isOk())
                .andReturn();

        List<String> setCookies = res.getResponse().getHeaders(HttpHeaders.SET_COOKIE);
        JsonNode body = objectMapper.readTree(res.getResponse().getContentAsByteArray());
        String accessToken = body.path("accessToken").asText();

        Cookie newRefreshCookie = cookieFromSetCookies(setCookies, "refresh_token");
        return new AuthSession(accessToken, newRefreshCookie, setCookies.isEmpty() ? null : setCookies.get(0));
    }

    public record AuthSession(String accessToken, Cookie refreshCookie, String setCookieHeader) {
    }

    private static Cookie cookieFromSetCookies(List<String> setCookieHeaders, String cookieName) {
        if (setCookieHeaders == null || setCookieHeaders.isEmpty()) {
            return null;
        }
        for (String h : setCookieHeaders) {
            Cookie c = cookieFromSetCookie(h, cookieName);
            if (c != null) {
                return c;
            }
        }
        return null;
    }

    private static Cookie cookieFromSetCookie(String setCookieHeader, String cookieName) {
        if (setCookieHeader == null || setCookieHeader.isBlank()) {
            return null;
        }
        String[] parts = setCookieHeader.split(";", 2);
        String[] nv = parts[0].split("=", 2);
        if (nv.length != 2) {
            return null;
        }
        String name = nv[0].trim();
        String value = nv[1];
        if (!cookieName.equals(name)) {
            return null;
        }
        return new Cookie(name, value);
    }
}
