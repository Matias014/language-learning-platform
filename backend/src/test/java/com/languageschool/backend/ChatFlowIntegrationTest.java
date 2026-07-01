package com.languageschool.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.languageschool.backend.dto.chatMessage.ChatMessageDto;
import com.languageschool.backend.entity.MessageSender;
import com.languageschool.backend.service.AiChatService;
import com.languageschool.backend.testsupport.AbstractIntegrationTest;
import com.languageschool.backend.testsupport.AuthTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class ChatFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockitoBean
    AiChatService aiChatService;

    @Test
    void user_can_create_session_post_message_list_messages_and_use_ai_endpoint() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email = login + "@example.com";
        var session = auth.register(login, email, "password");

        String createSessionJson = """
                {
                  "conversationLanguageCode": "en",
                  "title": "My session",
                  "systemPrompt": "You are helpful."
                }
                """;

        var created = mockMvc.perform(
                        post("/api/chat-sessions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType("application/json")
                                .content(createSessionJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.conversationLanguageCode").value("en"))
                .andReturn();

        JsonNode createdBody = objectMapper.readTree(created.getResponse().getContentAsByteArray());
        long chatSessionId = createdBody.path("id").asLong();

        String postMsgJson = """
                {
                  "message": "Hello!"
                }
                """;

        mockMvc.perform(
                        post("/api/chat-sessions/" + chatSessionId + "/messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType("application/json")
                                .content(postMsgJson)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.sender").value("user"))
                .andExpect(jsonPath("$.message").value("Hello!"));

        mockMvc.perform(
                        get("/api/chat-sessions/" + chatSessionId + "/messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].message").value("Hello!"));

        when(aiChatService.sendUserMessage(eq(chatSessionId), anyString(), anyString(), anyString()))
                .thenReturn(ChatMessageDto.builder()
                        .id(999L)
                        .sessionId(chatSessionId)
                        .sender(MessageSender.ai)
                        .message("AI says hi")
                        .sentAt(Instant.now())
                        .build());

        String aiJson = """
                {
                  "message": "Hi AI",
                  "systemPrompt": "Be concise"
                }
                """;

        mockMvc.perform(
                        post("/api/chat-sessions/" + chatSessionId + "/ai-messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + session.accessToken())
                                .contentType("application/json")
                                .content(aiJson)
                )
                .andExpect(status().isCreated())
                .andExpect(header().exists(HttpHeaders.LOCATION))
                .andExpect(jsonPath("$.sender").value("ai"))
                .andExpect(jsonPath("$.message").value("AI says hi"));
    }

    @Test
    void other_user_cannot_access_someone_elses_chat_session_403() throws Exception {
        var auth = new AuthTestClient(mockMvc, objectMapper);

        String login1 = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email1 = login1 + "@example.com";
        var u1 = auth.register(login1, email1, "password");

        String createSessionJson = """
                {
                  "conversationLanguageCode": "en",
                  "title": "Private",
                  "systemPrompt": "x"
                }
                """;

        var created = mockMvc.perform(
                        post("/api/chat-sessions")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + u1.accessToken())
                                .contentType("application/json")
                                .content(createSessionJson)
                )
                .andExpect(status().isCreated())
                .andReturn();

        long chatSessionId = objectMapper.readTree(created.getResponse().getContentAsByteArray()).path("id").asLong();

        String login2 = "u_" + UUID.randomUUID().toString().replace("-", "");
        String email2 = login2 + "@example.com";
        var u2 = auth.register(login2, email2, "password");

        mockMvc.perform(
                        get("/api/chat-sessions/" + chatSessionId)
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + u2.accessToken())
                )
                .andExpect(status().isForbidden());

        mockMvc.perform(
                        get("/api/chat-sessions/" + chatSessionId + "/messages")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + u2.accessToken())
                )
                .andExpect(status().isForbidden());
    }
}
