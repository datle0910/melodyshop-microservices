package com.melodyshop.order.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CouponDTO {
    private String id;
    private String code;
    private String type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private BigDecimal maxDiscount;
    private Integer maxUses;
    private Integer usedCount;
    private LocalDateTime expiresAt;
    private Boolean isActive;
    private BigDecimal discountAmount;
}
