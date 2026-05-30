package com.melodyshop.payment.service.gateway;

import com.melodyshop.payment.entity.PaymentTransaction;

import java.math.BigDecimal;

public interface PaymentGateway {
    String generateGatewayTransactionId();
    String buildPaymentUrl(PaymentTransaction transaction);
    String getProviderName();
}
