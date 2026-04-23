package com.melodyshop.engagement.entity;

import com.melodyshop.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_user_product", columnNames = {"user_id", "product_id"})
}, indexes = {
        @Index(name = "idx_review_product_created", columnList = "product_id, created_at")
})
public class Review extends BaseEntity {

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "product_id", nullable = false, length = 64)
    private String productId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    public Review() {
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

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
