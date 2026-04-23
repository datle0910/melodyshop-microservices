package com.melodyshop.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CartDTO {
    private String id;
    private String userId;
    private List<CartItemDTO> items;
    private BigDecimal totalAmount;
    private Integer totalItems;
}
