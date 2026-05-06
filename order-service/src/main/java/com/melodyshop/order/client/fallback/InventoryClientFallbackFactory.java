package com.melodyshop.order.client.fallback;

import com.melodyshop.order.client.InventoryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        log.error("InventoryClient fallback triggered due to: {}", cause.getMessage(), cause);
        return new InventoryClientFallback();
    }
}
