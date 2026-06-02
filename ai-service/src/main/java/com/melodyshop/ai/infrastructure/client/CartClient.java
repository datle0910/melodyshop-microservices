package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "CART-SERVICE")
public interface CartClient {

    @PostMapping("/api/cart/items")
    ApiResponse<CartItemDTO> addToCart(@RequestBody AddToCartRequest request);

    @GetMapping("/api/cart")
    ApiResponse<CartDTO> getCart(@RequestParam("userId") String userId);

    record AddToCartRequest(
            String userId,
            String productId,
            String productName,
            String productImage,
            String variantId,
            String variantName,
            String sku,
            Double unitPrice,
            Integer quantity
    ) {}

    record CartItemDTO(
            String id,
            String productId,
            String productName,
            String productImage,
            String variantId,
            String variantName,
            String sku,
            Double unitPrice,
            Integer quantity,
            Double subtotal
    ) {}

    record CartDTO(
            String id,
            String userId,
            java.util.List<CartItemDTO> items,
            Integer totalItems,
            Double totalAmount
    ) {}
}
