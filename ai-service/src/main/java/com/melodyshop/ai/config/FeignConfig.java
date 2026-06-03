package com.melodyshop.ai.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.melodyshop.ai.infrastructure.client")
public class FeignConfig {
    // Feign clients are auto-configured to use Eureka for service discovery
    // with spring.cloud.discovery.enabled=true (default)
}
