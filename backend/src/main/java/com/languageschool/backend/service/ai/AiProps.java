package com.languageschool.backend.service.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiProps {
    @Value("${anthropic.api.baseUrl:https://api.anthropic.com}")
    private String baseUrl;

    @Value("${anthropic.api.key:}")
    private String apiKey;

    @Value("${anthropic.api.version:2023-06-01}")
    private String anthropicVersion;

    @Value("${anthropic.model:claude-3-5-haiku-latest}")
    private String model;

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public String anthropicVersion() {
        return anthropicVersion;
    }

    public String model() {
        return model;
    }
}
