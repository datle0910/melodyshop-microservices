package com.melodyshop.user.client.fallback;

import com.melodyshop.user.client.OrderClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {

    @Override
    public OrderClient create(Throwable cause) {
        log.error("OrderClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new OrderClientFallback();
    }
}
