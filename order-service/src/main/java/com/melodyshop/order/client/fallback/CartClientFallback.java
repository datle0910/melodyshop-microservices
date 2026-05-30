package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.CartClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CartClientFallback implements CartClient {

    @Override
    public ApiResponse<Void> clearCart(String userId) {
        log.warn("Fallback: Cart clear skipped for user {}", userId);
        return ApiResponse.ok("Cart clear skipped (service unavailable)", null);
    }
}
