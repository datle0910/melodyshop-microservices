package com.melodyshop.inventory.client;

import com.melodyshop.common.dto.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "product-service", fallback = ProductClientFallback.class)
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductDTO> getProductById(@PathVariable("id") String id);

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class ProductDTO {
        private String id;
        private String name;
        private BigDecimal price;
    }
}
