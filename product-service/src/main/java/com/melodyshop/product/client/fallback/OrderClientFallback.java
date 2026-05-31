package com.melodyshop.product.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.product.client.OrderClient;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrderClientFallback implements OrderClient {

    @Override
    public ApiResponse<Boolean> hasOrdersByProductId(String productId) {
        log.warn("Fallback: Assuming product {} has orders (service unavailable)", productId);
        return ApiResponse.ok(true);
    }
}
