package com.melodyshop.auth.client;

import com.melodyshop.auth.config.feign.NotificationServiceClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "notification-service", fallbackFactory = NotificationServiceClientFallbackFactory.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/welcome")
    void sendWelcomeEmail(@RequestParam("email") String email, @RequestParam("fullName") String fullName);
}
