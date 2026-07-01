package com.languageschool.backend.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiRuntimeLimits.class)
public class AiRuntimeConfig {
}
