package com.melodyshop.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "gemini")
public class GeminiProperties {

    private String apiKey;
    private String model = "gemini-2.0-flash";
    private int maxOutputTokens = 2048;
    private double temperature = 0.7;
    private String baseUrl = "https://generativelanguage.googleapis.com";
}
