package com.melodyshop.engagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AddWishlistItemRequest {

    @NotBlank(message = "productId không được để trống")
    @Size(max = 64)
    private String productId;

    public AddWishlistItemRequest() {
    }

    public AddWishlistItemRequest(String productId) {
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }
}
