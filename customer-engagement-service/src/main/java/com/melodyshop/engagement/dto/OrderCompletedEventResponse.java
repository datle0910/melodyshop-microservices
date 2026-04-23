package com.melodyshop.engagement.dto;

public class OrderCompletedEventResponse {
    private String orderId;
    private String userId;
    private int processedProducts;

    public OrderCompletedEventResponse() {
    }

    public OrderCompletedEventResponse(String orderId, String userId, int processedProducts) {
        this.orderId = orderId;
        this.userId = userId;
        this.processedProducts = processedProducts;
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

    public int getProcessedProducts() {
        return processedProducts;
    }

    public void setProcessedProducts(int processedProducts) {
        this.processedProducts = processedProducts;
    }
}
