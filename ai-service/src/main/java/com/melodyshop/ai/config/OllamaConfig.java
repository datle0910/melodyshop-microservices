package com.melodyshop.ai.config;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!no-tools")
public class OllamaConfig {

    @Value("${langchain4j.ollama.base-url:${OLLAMA_BASE_URL:http://host.docker.internal:11434}}")
    private String baseUrl;

    @Value("${langchain4j.ollama.chat-model:${OLLAMA_MODEL:llama3.2}}")
    private String model;

    @Value("${langchain4j.ollama.temperature:${OLLAMA_TEMPERATURE:0.7}}")
    private double temperature;

    @Value("${langchain4j.ollama.timeout:${OLLAMA_TIMEOUT:120}s}")
    private String timeoutStr;

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        int timeoutSeconds = 120;
        try {
            String timeout = timeoutStr.replace("s", "").trim();
            timeoutSeconds = Integer.parseInt(timeout);
        } catch (Exception e) {
            // use default
        }

        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(model)
                .temperature(temperature)
                .timeout(java.time.Duration.ofSeconds(timeoutSeconds))
                .build();
    }
}
