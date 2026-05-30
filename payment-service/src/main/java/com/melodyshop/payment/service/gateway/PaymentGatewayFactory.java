package com.melodyshop.payment.service.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentGatewayFactory {

    private final VnpayGatewayService vnpayGatewayService;
    private final MomoGatewayService momoGatewayService;
    private final StripeGatewayService stripeGatewayService;
    private final MockGatewayService mockGatewayService;

    public PaymentGateway getGateway(String provider) {
        if (provider == null) {
            return mockGatewayService;
        }
        return switch (provider.toUpperCase()) {
            case "VNPAY" -> vnpayGatewayService;
            case "MOMO" -> momoGatewayService;
            case "STRIPE" -> stripeGatewayService;
            default -> mockGatewayService;
        };
    }
}
