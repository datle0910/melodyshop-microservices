package com.melodyshop.product.config.feign;

import com.melodyshop.product.client.InventoryClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        log.error("Fallback factory activated for InventoryClient: {}", cause.getMessage());
        return new InventoryClientFallback();
    }
}
