package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.dto.PaymentResultRequest;
import com.melodyshop.order.service.InternalServiceTokenValidator;
import com.melodyshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/orders")
@RequiredArgsConstructor
public class InternalOrderPaymentController {

    private final OrderService orderService;
    private final InternalServiceTokenValidator internalTokenValidator;

    @PostMapping("/{orderId}/payment-success")
    public ResponseEntity<ApiResponse<Void>> markPaymentSuccess(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable String orderId,
            @RequestBody PaymentResultRequest request) {
        internalTokenValidator.requireValid(internalToken);
        orderService.handlePaymentSuccess(orderId, request.getPaymentId());
        return ResponseEntity.ok(ApiResponse.ok("Order đã đồng bộ payment success", null));
    }

    @PostMapping("/{orderId}/payment-failed")
    public ResponseEntity<ApiResponse<Void>> markPaymentFailed(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable String orderId,
            @RequestBody PaymentResultRequest request) {
        internalTokenValidator.requireValid(internalToken);
        orderService.handlePaymentFailure(orderId, request.getPaymentId(), false);
        return ResponseEntity.ok(ApiResponse.ok("Order đã đồng bộ payment failed", null));
    }

    @PostMapping("/{orderId}/payment-expired")
    public ResponseEntity<ApiResponse<Void>> markPaymentExpired(
            @RequestHeader("X-Internal-Service-Token") String internalToken,
            @PathVariable String orderId,
            @RequestBody PaymentResultRequest request) {
        internalTokenValidator.requireValid(internalToken);
        orderService.handlePaymentFailure(orderId, request.getPaymentId(), true);
        return ResponseEntity.ok(ApiResponse.ok("Order đã đồng bộ payment expired", null));
    }
}
