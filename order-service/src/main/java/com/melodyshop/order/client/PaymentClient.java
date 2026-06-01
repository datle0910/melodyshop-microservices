package com.melodyshop.order.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.fallback.PaymentClientFallbackFactory;
import com.melodyshop.order.dto.CreatePaymentRequest;
import com.melodyshop.order.dto.CreatePaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "payment-service", fallbackFactory = PaymentClientFallbackFactory.class)
public interface PaymentClient {

    @PostMapping("/api/payments")
    ApiResponse<CreatePaymentResponse> createPayment(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody CreatePaymentRequest request);

    @GetMapping("/api/payments/{paymentId}")
    ApiResponse<Object> getPaymentStatus(@PathVariable("paymentId") String paymentId);
}
