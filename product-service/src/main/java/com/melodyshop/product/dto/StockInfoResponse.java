package com.melodyshop.product.dto;

import lombok.*;

/**
 * Stock info response - used by product-service to display stock on product pages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockInfoResponse {
    private String sku;
    private Integer quantity;
    private Integer reservedQuantity;
    private Integer availableQuantity;
    private Boolean lowStock;
}
