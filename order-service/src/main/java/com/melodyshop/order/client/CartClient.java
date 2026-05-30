package com.melodyshop.order.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.fallback.CartClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "cart-service", fallbackFactory = CartClientFallbackFactory.class)
public interface CartClient {

    @DeleteMapping("/api/cart")
    ApiResponse<Void> clearCart(@RequestHeader("X-User-Id") String userId);
}
