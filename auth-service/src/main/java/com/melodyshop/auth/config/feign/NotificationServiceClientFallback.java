package com.melodyshop.auth.config.feign;

import com.melodyshop.auth.client.NotificationServiceClient;
import com.melodyshop.auth.dto.OtpRequest;
import com.melodyshop.auth.dto.WelcomeRequest;
import com.melodyshop.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class NotificationServiceClientFallback implements NotificationServiceClient {

    @Override
    public ApiResponse<Map<String, String>> sendWelcomeEmail(WelcomeRequest request) {
        log.warn("Fallback: Notification service unavailable, skipping welcome email for: {}", request.getEmail());
        return ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("Notification service temporarily unavailable")
                .build();
    }

    @Override
    public ApiResponse<Map<String, String>> sendOtp(OtpRequest request) {
        log.warn("Fallback: Notification service unavailable, skipping OTP email for: {}", request.getTo());
        return ApiResponse.<Map<String, String>>builder()
                .success(false)
                .message("MAIL_SERVICE_UNAVAILABLE")
                .build();
    }
}
