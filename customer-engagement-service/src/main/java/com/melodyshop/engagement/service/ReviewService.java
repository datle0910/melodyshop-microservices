package com.melodyshop.engagement.service;

import com.melodyshop.engagement.dto.CreateReviewRequest;
import com.melodyshop.engagement.dto.ProductReviewsResponse;
import com.melodyshop.engagement.dto.ReviewResponse;

public interface ReviewService {
    ReviewResponse createReview(String userId, CreateReviewRequest request);
    ProductReviewsResponse getReviewsByProduct(String productId);
}
