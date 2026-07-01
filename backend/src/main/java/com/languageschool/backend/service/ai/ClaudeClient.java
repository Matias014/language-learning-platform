package com.languageschool.backend.service.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${anthropic.api.key:}')")
public class ClaudeClient {

    private final RestClient http;
    private final String model;
    private final String messagesPath;

    public ClaudeClient(RestClient.Builder builder, AiProps props) {
        this.model = props.model();
        String base = props.baseUrl() == null ? "" : props.baseUrl().trim();
        String normalizedBase = base.endsWith("/v1") ? base.substring(0, base.length() - 3) : base;
        this.messagesPath = "/v1/messages";
        this.http = builder
                .baseUrl(normalizedBase.isEmpty() ? "https://api.anthropic.com" : normalizedBase)
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("anthropic-version", props.anthropicVersion())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public CompletionResult completeWithUsage(String systemPrompt,
                                              List<Map<String, Object>> messages,
                                              int maxOutputTokens) throws RestClientException {
        return completeWithUsage(systemPrompt, messages, maxOutputTokens, null);
    }

    public CompletionResult completeWithUsage(String systemPrompt,
                                              List<Map<String, Object>> messages,
                                              int maxOutputTokens,
                                              Map<String, Object> metadata) throws RestClientException {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("system", systemPrompt);
        body.put("max_tokens", maxOutputTokens);
        body.put("messages", messages);
        if (metadata != null && !metadata.isEmpty()) {
            body.put("metadata", metadata);
        }

        int attempts = 3;
        RestClientException last = null;
        for (int i = 0; i < attempts; i++) {
            try {
                ClaudeResponse resp = this.http
                        .post()
                        .uri(messagesPath)
                        .body(body)
                        .retrieve()
                        .body(ClaudeResponse.class);

                if (resp == null || resp.content() == null || resp.content().isEmpty()) {
                    return new CompletionResult("", 0, 0);
                }

                StringBuilder sb = new StringBuilder();
                for (ContentBlock cb : resp.content()) {
                    if ("text".equalsIgnoreCase(cb.type())
                            && cb.text() != null
                            && !cb.text().isBlank()) {
                        if (sb.length() > 0) {
                            sb.append("\n\n");
                        }
                        sb.append(cb.text());
                    }
                }
                String text = sb.length() == 0 ? "" : sb.toString();

                Integer inTok = resp.usage() != null ? resp.usage().input_tokens() : null;
                Integer outTok = resp.usage() != null ? resp.usage().output_tokens() : null;

                return new CompletionResult(text, inTok, outTok);
            } catch (RestClientResponseException r) {
                last = r;
                int s = r.getRawStatusCode();
                if (s == 429 || s >= 500) {
                    backoff(i);
                    continue;
                }
                throw r;
            } catch (RestClientException e) {
                last = e;
                backoff(i);
            }
        }
        throw last == null ? new RestClientException("LLM_CALL_FAILED") {
        } : last;
    }

    private void backoff(int attempt) {
        long ms = Math.min(2000L * (attempt + 1), 6000L);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    public record CompletionResult(String text, Integer promptTokens, Integer completionTokens) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClaudeResponse(String id, String type, List<ContentBlock> content, Usage usage) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ContentBlock(String type, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Usage(Integer input_tokens, Integer output_tokens) {
    }
}
