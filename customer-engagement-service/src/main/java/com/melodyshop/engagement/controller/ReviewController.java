package com.melodyshop.engagement.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.engagement.dto.CreateReviewRequest;
import com.melodyshop.engagement.dto.ProductReviewsResponse;
import com.melodyshop.engagement.dto.ReviewResponse;
import com.melodyshop.engagement.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping("/reviews")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewResponse response = reviewService.createReview(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response));
    }

    @GetMapping("/products/{productId}/reviews")
    public ResponseEntity<ApiResponse<ProductReviewsResponse>> getReviewsByProduct(@PathVariable String productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getReviewsByProduct(productId)));
    }
}
