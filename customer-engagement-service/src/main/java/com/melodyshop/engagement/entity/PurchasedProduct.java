package com.melodyshop.engagement.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "purchased_products", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchased_user_product", columnNames = {"user_id", "product_id"})
}, indexes = {
        @Index(name = "idx_purchased_user_product", columnList = "user_id, product_id")
})
public class PurchasedProduct extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    public PurchasedProduct() {
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
