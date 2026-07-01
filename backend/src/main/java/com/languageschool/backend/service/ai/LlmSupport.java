package com.languageschool.backend.service.ai;

import com.languageschool.backend.entity.LlmStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.regex.Pattern;

public final class LlmSupport {

    private static final Pattern EMAIL = Pattern.compile("(?i)[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}");
    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._-]+");
    private static final Pattern JWT = Pattern.compile("[A-Za-z0-9-_]{10,}\\.[A-Za-z0-9-_]{10,}\\.[A-Za-z0-9-_]{10,}");
    private static final Pattern KV_SECRET = Pattern.compile("(?i)(secret|password|pass|token)\\s*[:=]\\s*[^\\s\\n]+");

    private LlmSupport() {
    }

    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    public static String redact(String s) {
        if (s == null || s.isBlank()) {
            return "";
        }
        String r = EMAIL.matcher(s).replaceAll("[REDACTED_EMAIL]");
        r = BEARER.matcher(r).replaceAll("Bearer [REDACTED_TOKEN]");
        r = JWT.matcher(r).replaceAll("[REDACTED_JWT]");
        r = KV_SECRET.matcher(r).replaceAll("$1=[REDACTED]");
        return r;
    }

    public static String formatUsageSuffix(ClaudeClient.CompletionResult result) {
        if (result == null) {
            return "";
        }
        Integer in = result.promptTokens();
        Integer out = result.completionTokens();
        String si = String.valueOf(in == null ? "" : in);
        String so = String.valueOf(out == null ? "" : out);
        if (si.isBlank() && so.isBlank()) {
            return "";
        }
        return "\n[usage in=" + si + " out=" + so + "]";
    }

    public static LlmStatus mapStatus(RestClientException ex) {
        if (ex instanceof ResourceAccessException) {
            return LlmStatus.timeout;
        }
        if (ex instanceof RestClientResponseException r) {
            int s = r.getRawStatusCode();
            if (s == 400) return LlmStatus.invalid_request;
            if (s == 401 || s == 403) return LlmStatus.safety_block;
            if (s == 408 || s == 504) return LlmStatus.timeout;
            if (s == 429) return LlmStatus.rate_limited;
            if (s == 402) return LlmStatus.quota_exceeded;
            if (s >= 500) return LlmStatus.server_error;
        }
        return LlmStatus.server_error;
    }
}
