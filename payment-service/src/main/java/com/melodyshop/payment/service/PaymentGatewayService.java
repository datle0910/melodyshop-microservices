package com.melodyshop.payment.service;

import com.melodyshop.payment.entity.PaymentTransaction;

public interface PaymentGatewayService {
    String generateGatewayTransactionId();
    String buildRedirectUrl(PaymentTransaction transaction);
}
