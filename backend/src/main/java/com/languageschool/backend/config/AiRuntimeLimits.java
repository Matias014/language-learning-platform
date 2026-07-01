package com.languageschool.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "ai.limits")
public record AiRuntimeLimits(
        @DefaultValue("1200") int maxUserMessageChars,
        @DefaultValue("2000") int maxSystemPromptChars,
        @DefaultValue("2400") int maxOutputTokens,
        @DefaultValue("3") int maxHints
) {
}
