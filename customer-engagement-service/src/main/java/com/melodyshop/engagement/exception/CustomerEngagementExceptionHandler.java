package com.melodyshop.engagement.exception;

import com.melodyshop.common.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.melodyshop.engagement")
public class CustomerEngagementExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomerEngagementExceptionHandler.class);

    @ExceptionHandler(AlreadyReviewedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAlreadyReviewed(AlreadyReviewedException ex) {
        LOGGER.warn("Rejected duplicate review: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage(), "ALREADY_REVIEWED"));
    }

    @ExceptionHandler(ProductNotPurchasedException.class)
    public ResponseEntity<ApiResponse<Void>> handleProductNotPurchased(ProductNotPurchasedException ex) {
        LOGGER.warn("Rejected review for non-purchased product: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage(), "PRODUCT_NOT_PURCHASED"));
    }
}
