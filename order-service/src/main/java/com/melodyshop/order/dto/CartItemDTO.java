package com.melodyshop.order.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartItemDTO {
    private String id;
    private String productId;
    private String variantId;
    private String sku;
    private String productName;
    private String variantName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;
}
