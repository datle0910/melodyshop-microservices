package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.CartClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CartClientFallbackFactory implements FallbackFactory<CartClient> {

    @Override
    public CartClient create(Throwable cause) {
        String reason = cause != null && cause.getMessage() != null
                ? cause.getMessage()
                : "unknown error";
        log.error("CartClient fallback triggered due to: {}", reason, cause);
        return new CartClientFallback(reason);
    }
}
