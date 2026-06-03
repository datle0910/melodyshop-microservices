package com.melodyshop.engagement.service;

import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.engagement.dto.CreateReviewRequest;
import com.melodyshop.engagement.dto.ProductReviewsResponse;
import com.melodyshop.engagement.dto.ReviewResponse;
import com.melodyshop.engagement.dto.UpdateReviewRequest;

public interface ReviewService {
    ReviewResponse createReview(String userId, CreateReviewRequest request);
    ReviewResponse updateReview(String reviewId, String userId, UpdateReviewRequest request);
    ProductReviewsResponse getReviewsByProduct(String productId);
    PageResponse<com.melodyshop.engagement.dto.AdminReviewResponse> getAllReviews(int page, int size);
    void deleteReview(String reviewId);
}
