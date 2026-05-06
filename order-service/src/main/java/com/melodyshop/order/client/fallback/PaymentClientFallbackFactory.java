package com.melodyshop.order.client.fallback;

import com.melodyshop.order.client.PaymentClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentClientFallbackFactory implements FallbackFactory<PaymentClient> {

    @Override
    public PaymentClient create(Throwable cause) {
        log.error("PaymentClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new PaymentClientFallback();
    }
}
