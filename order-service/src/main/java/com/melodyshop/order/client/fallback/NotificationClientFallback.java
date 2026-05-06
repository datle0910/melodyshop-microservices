package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.NotificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationClientFallback implements NotificationClient {

    @Override
    public ApiResponse<Void> sendOrderConfirmation(String email, String orderNumber, String totalAmount, String customerName) {
        log.warn("Fallback activated for sendOrderConfirmation - email: {}, orderNumber: {}", email, orderNumber);
        return ApiResponse.error("Notification service temporarily unavailable");
    }

    @Override
    public ApiResponse<Void> sendOrderStatusUpdate(String email, String orderNumber, String newStatus, String customerName) {
        log.warn("Fallback activated for sendOrderStatusUpdate - email: {}, orderNumber: {}, newStatus: {}", 
                email, orderNumber, newStatus);
        return ApiResponse.error("Notification service temporarily unavailable");
    }
}
