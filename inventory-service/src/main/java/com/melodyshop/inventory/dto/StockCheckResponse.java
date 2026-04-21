package com.melodyshop.inventory.dto;

import lombok.*;

/**
 * Response DTO cho kiểm tra tồn kho.
 */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class StockCheckResponse {
    private String sku;
    private Integer availableQuantity;
    private Boolean inStock;
}
