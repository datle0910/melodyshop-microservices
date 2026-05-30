package com.melodyshop.payment.service.impl;

import com.melodyshop.payment.entity.PaymentTransaction;
import com.melodyshop.payment.service.gateway.PaymentGateway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class MockPaymentGatewayServiceImpl implements PaymentGateway {

    @Value("${payment.gateway.redirect-base-url:http://fake-gateway/pay}")
    private String redirectBaseUrl;

    @Override
    public String generateGatewayTransactionId() {
        return "MOCK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    @Override
    public String buildPaymentUrl(PaymentTransaction transaction) {
        return redirectBaseUrl + "/" + transaction.getGatewayTransactionId() + "?paymentId=" + transaction.getId();
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }
}
