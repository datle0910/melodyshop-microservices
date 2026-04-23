package com.melodyshop.engagement.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateReviewRequest {

    @NotBlank(message = "productId không được để trống")
    @Size(max = 64)
    private String productId;

    @Min(value = 1, message = "rating phải từ 1 đến 5")
    @Max(value = 5, message = "rating phải từ 1 đến 5")
    private int rating;

    @Size(max = 1000, message = "comment tối đa 1000 ký tự")
    private String comment;

    public CreateReviewRequest() {
    }

    public CreateReviewRequest(String productId, int rating, String comment) {
        this.productId = productId;
        this.rating = rating;
        this.comment = comment;
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
