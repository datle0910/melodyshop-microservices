package com.melodyshop.user.client;
import com.melodyshop.common.dto.ApiResponse;

import com.melodyshop.user.client.fallback.OrderClientFallback;
import com.melodyshop.user.client.fallback.OrderClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {

    @GetMapping("/api/orders/has-orders")
    ApiResponse<Boolean> hasOrdersByUserId(@RequestParam("userId") String userId);
}
