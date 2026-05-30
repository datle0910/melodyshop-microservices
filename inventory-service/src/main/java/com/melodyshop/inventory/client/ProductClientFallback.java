package com.melodyshop.inventory.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ProductClientFallback implements ProductClient {
    @Override
    public ApiResponse<ProductDTO> getProductById(String id) {
        return ApiResponse.ok(ProductDTO.builder()
                .id(id)
                .name("Unknown")
                .price(BigDecimal.ZERO)
                .build());
    }
}
