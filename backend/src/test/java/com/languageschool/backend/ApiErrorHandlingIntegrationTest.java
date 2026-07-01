package com.languageschool.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ApiErrorHandlingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void unauthorized_returns_401_with_code() throws Exception {
        mockMvc.perform(get("/api/users/me/chat-sessions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void not_found_returns_404_with_code() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        mockMvc.perform(
                        get("/api/chat-sessions/999999999")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
