package com.melodyshop.order.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.fallback.NotificationClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "notification-service", fallbackFactory = NotificationClientFallbackFactory.class)
public interface NotificationClient {

    @PostMapping("/api/notifications/order-confirmed")
    ApiResponse<Void> sendOrderConfirmation(
            @RequestParam("email") String email,
            @RequestParam("orderNumber") String orderNumber,
            @RequestParam("totalAmount") String totalAmount,
            @RequestParam("customerName") String customerName);

    @PostMapping("/api/notifications/order-status")
    ApiResponse<Void> sendOrderStatusUpdate(
            @RequestParam("email") String email,
            @RequestParam("orderNumber") String orderNumber,
            @RequestParam("newStatus") String newStatus,
            @RequestParam("customerName") String customerName);
}
