package com.melodyshop.order.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "wishlists")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Wishlist extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;
}
