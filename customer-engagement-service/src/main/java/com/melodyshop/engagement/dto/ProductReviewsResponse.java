package com.melodyshop.engagement.dto;

import java.util.List;

public class ProductReviewsResponse {
    private String productId;
    private double average;
    private long total;
    private List<ReviewResponse> reviews;

    public ProductReviewsResponse() {
    }

    public ProductReviewsResponse(String productId, double average, long total, List<ReviewResponse> reviews) {
        this.productId = productId;
        this.average = average;
        this.total = total;
        this.reviews = reviews;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public double getAverage() {
        return average;
    }

    public void setAverage(double average) {
        this.average = average;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<ReviewResponse> getReviews() {
        return reviews;
    }

    public void setReviews(List<ReviewResponse> reviews) {
        this.reviews = reviews;
    }
}
