package com.melodyshop.payment.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.payment.dto.PaymentOrderRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "order-service")
public interface OrderClient {

    @PostMapping("/api/internal/orders/{orderId}/payment-success")
    ApiResponse<Void> markPaymentSuccess(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable("orderId") String orderId,
            @RequestBody PaymentOrderRequest request);

    @PostMapping("/api/internal/orders/{orderId}/payment-failed")
    ApiResponse<Void> markPaymentFailed(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable("orderId") String orderId,
            @RequestBody PaymentOrderRequest request);

    @PostMapping("/api/internal/orders/{orderId}/payment-expired")
    ApiResponse<Void> markPaymentExpired(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable("orderId") String orderId,
            @RequestBody PaymentOrderRequest request);
}
