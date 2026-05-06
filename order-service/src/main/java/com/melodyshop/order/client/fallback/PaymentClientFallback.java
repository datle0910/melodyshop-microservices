package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.PaymentClient;
import com.melodyshop.order.dto.CreatePaymentRequest;
import com.melodyshop.order.dto.CreatePaymentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PaymentClientFallback implements PaymentClient {

    @Override
    public ApiResponse<CreatePaymentResponse> createPayment(CreatePaymentRequest request) {
        log.warn("Fallback activated for createPayment - orderId: {}", request != null ? request.getOrderId() : "null");
        return ApiResponse.<CreatePaymentResponse>error("Payment service temporarily unavailable");
    }

    @Override
    public ApiResponse<Object> getPaymentStatus(String paymentId) {
        log.warn("Fallback activated for getPaymentStatus - paymentId: {}", paymentId);
        return ApiResponse.error("Payment service temporarily unavailable");
    }
}
