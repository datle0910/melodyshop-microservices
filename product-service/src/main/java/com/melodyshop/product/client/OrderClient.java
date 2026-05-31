package com.melodyshop.product.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.product.client.fallback.OrderClientFallback;
import com.melodyshop.product.client.fallback.OrderClientFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "order-service", fallbackFactory = OrderClientFallbackFactory.class)
public interface OrderClient {

    @GetMapping("/api/orders/has-product-orders")
    ApiResponse<Boolean> hasOrdersByProductId(@RequestParam("productId") String productId);
}
