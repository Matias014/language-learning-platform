package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void register_then_refresh_then_logout() throws Exception {
        AuthTestClient auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + java.util.UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";

        var reg = auth.register(login, email, "password");

        assertThat(reg.accessToken()).isNotBlank();
        assertThat(reg.refreshCookie()).isNotNull();

        var refreshed = auth.refresh(reg.refreshCookie());
        assertThat(refreshed.accessToken()).isNotBlank();
        assertThat(refreshed.refreshCookie()).isNotNull();

        mockMvc.perform(
                        post("/api/auth/logout")
                                .cookie(refreshed.refreshCookie())
                )
                .andExpect(status().isNoContent())
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void refresh_without_cookie_returns_401_and_clears_cookie() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(header().exists(HttpHeaders.SET_COOKIE));
    }

    @Test
    void login_admin_seeded_user_ok() throws Exception {
        AuthTestClient auth = new AuthTestClient(mockMvc, objectMapper);
        var session = auth.login("admin", "password");
        assertThat(session.accessToken()).isNotBlank();
        assertThat(session.refreshCookie()).isNotNull();
    }
}
