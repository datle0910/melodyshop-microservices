package com.melodyshop.order.entity;

import com.melodyshop.common.entity.BaseEntity;
import com.melodyshop.order.entity.enums.CouponType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "coupons")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Coupon extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CouponType type;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal value;

    @Column(name = "min_order_amount", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 15, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "max_uses")
    @Builder.Default
    private Integer maxUses = 0;

    @Column(name = "used_count")
    @Builder.Default
    private Integer usedCount = 0;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
}
