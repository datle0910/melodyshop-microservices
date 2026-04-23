package com.melodyshop.engagement.exception;

public class ProductNotPurchasedException extends RuntimeException {
    public ProductNotPurchasedException(String productId) {
        super("Người dùng chưa mua sản phẩm nên không thể review: " + productId);
    }
}
