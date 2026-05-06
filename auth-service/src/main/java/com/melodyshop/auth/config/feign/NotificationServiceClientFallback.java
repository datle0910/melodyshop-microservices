package com.melodyshop.auth.config.feign;

import com.melodyshop.auth.client.NotificationServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public void sendWelcomeEmail(String email, String fullName) {
        log.warn("Fallback activated: Notification service temporarily unavailable, skipping welcome email for: {}", email);
    }
}
