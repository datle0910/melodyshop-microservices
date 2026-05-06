package com.melodyshop.order.config.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Logger;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FeignClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class FeignConfig {

    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public Request.Options feignOptions() {
        return new Request.Options(
                5000,  // connect timeout in milliseconds
                15000  // read timeout in milliseconds
        );
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }

    @Slf4j
    static class CustomErrorDecoder implements ErrorDecoder {
        private final ObjectMapper objectMapper = new ObjectMapper();

        @Override
        public Exception decode(String methodKey, Response response) {
            try {
                if (response.body() != null) {
                    String body = new String(response.body().asInputStream().readAllBytes());
                    log.error("Feign error for method {}: status={}, body={}", methodKey, response.status(), body);
                } else {
                    log.error("Feign error for method {}: status={}, no body", methodKey, response.status());
                }
            } catch (IOException e) {
                log.error("Error reading feign response body", e);
            }

            return new FeignClientException(
                    response.status(),
                    "Feign client error: " + methodKey + " returned status " + response.status()
            );
        }
    }

    static class FeignClientException extends RuntimeException {
        private final int status;

        public FeignClientException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }
}
