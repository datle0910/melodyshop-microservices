package com.melodyshop.order.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "variant_id", length = 36)
    private String variantId;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
