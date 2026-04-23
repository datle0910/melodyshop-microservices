package com.melodyshop.engagement.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "wishlist_items", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wishlist_user_product", columnNames = {"user_id", "product_id"})
}, indexes = {
        @Index(name = "idx_wishlist_user_created", columnList = "user_id, created_at")
})
public class WishlistItem extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    public WishlistItem() {
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
