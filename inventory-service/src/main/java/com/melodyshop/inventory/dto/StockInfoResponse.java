package com.melodyshop.inventory.dto;

import lombok.*;

/**
 * Response DTO cho stock info - lightweight response
 * (khong can quantity khi chi can hien thi trang thai ton kho).
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
