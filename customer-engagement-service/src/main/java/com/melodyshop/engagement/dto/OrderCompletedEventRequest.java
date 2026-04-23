package com.melodyshop.engagement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public class OrderCompletedEventRequest {

    @NotBlank(message = "orderId không được để trống")
    @Size(max = 64)
    private String orderId;

    @NotBlank(message = "userId không được để trống")
    @Size(max = 64)
    private String userId;

    @NotEmpty(message = "productIds không được rỗng")
    private List<String> productIds;

    public OrderCompletedEventRequest() {
    }

    public OrderCompletedEventRequest(String orderId, String userId, List<String> productIds) {
        this.orderId = orderId;
        this.userId = userId;
        this.productIds = productIds;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public List<String> getProductIds() {
        return productIds;
    }

    public void setProductIds(List<String> productIds) {
        this.productIds = productIds;
    }
}
