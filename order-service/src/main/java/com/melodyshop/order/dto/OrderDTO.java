package com.melodyshop.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class OrderDTO {
    private String id;
    private String orderCode;
    private String userId;
    private String status;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal shippingFee;
    private BigDecimal totalAmount;
    private String couponCode;
    private String receiverName;
    private String receiverPhone;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingAddress;
    private String note;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
