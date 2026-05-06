package com.melodyshop.cart.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "carts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<com.melodyshop.cart.entity.CartItem> items = new java.util.ArrayList<>();

    @Column(name = "total_amount", precision = 12)
    @Builder.Default
    private BigDecimal totalAmount = BigDecimal.ZERO;
}
