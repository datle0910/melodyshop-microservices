package com.melodyshop.engagement.exception;

public class AlreadyReviewedException extends RuntimeException {
    public AlreadyReviewedException(String productId) {
        super("Người dùng đã review sản phẩm này: " + productId);
    }
}
