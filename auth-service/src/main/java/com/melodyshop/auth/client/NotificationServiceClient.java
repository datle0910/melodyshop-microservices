package com.melodyshop.auth.client;

import com.melodyshop.auth.config.feign.NotificationServiceClientFallbackFactory;
import com.melodyshop.auth.dto.OtpRequest;
import com.melodyshop.auth.dto.WelcomeRequest;
import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Map;

@FeignClient(name = "notification-service", fallbackFactory = NotificationServiceClientFallbackFactory.class)
public interface NotificationServiceClient {

    @PostMapping("/api/notifications/welcome")
    ApiResponse<Map<String, String>> sendWelcomeEmail(@RequestBody WelcomeRequest request);

    @PostMapping("/api/notifications/otp")
    ApiResponse<Map<String, String>> sendOtp(@RequestBody OtpRequest request);
}
