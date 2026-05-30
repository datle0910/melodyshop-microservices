package com.melodyshop.payment.service.gateway;

import com.melodyshop.payment.entity.PaymentTransaction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Slf4j
public class MockGatewayService implements PaymentGateway {

    @Override
    public String generateGatewayTransactionId() {
        return "MOCK_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public String buildPaymentUrl(PaymentTransaction transaction) {
        return "http://fake-gateway/pay/" + transaction.getId() + "?gatewayTxId=" + transaction.getGatewayTransactionId();
    }

    @Override
    public String getProviderName() {
        return "MOCK";
    }
}
