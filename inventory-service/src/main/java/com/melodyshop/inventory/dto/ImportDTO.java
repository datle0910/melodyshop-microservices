package com.melodyshop.inventory.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportDTO {

    private String id;
    private String importCode;
    private String note;
    private String importedBy;
    private Integer totalQuantity;
    private List<ImportItemDTO> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportItemDTO {
        private String id;
        private String sku;
        private String productId;
        private String variantId;
        private String productName;
        private Integer quantityBefore;
        private Integer quantityAfter;
        private Integer quantityAdded;
        private java.math.BigDecimal importPrice;
    }
}
