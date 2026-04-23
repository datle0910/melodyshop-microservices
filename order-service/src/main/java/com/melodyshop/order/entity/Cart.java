package com.melodyshop.order.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cart extends BaseEntity {

    @Column(name = "user_id", nullable = false, unique = true, length = 36)
    private String userId;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
}
