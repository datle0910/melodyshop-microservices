package com.melodyshop.engagement.service.impl;

import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.engagement.client.ProductClient;
import com.melodyshop.engagement.dto.AdminReviewResponse;
import com.melodyshop.engagement.dto.CreateReviewRequest;
import com.melodyshop.engagement.dto.ProductReviewsResponse;
import com.melodyshop.engagement.dto.ReviewResponse;
import com.melodyshop.engagement.entity.Review;
import com.melodyshop.engagement.exception.AlreadyReviewedException;
import com.melodyshop.engagement.exception.ProductNotPurchasedException;
import com.melodyshop.engagement.repository.ReviewRepository;
import com.melodyshop.engagement.service.PurchaseEligibilityService;
import com.melodyshop.engagement.service.ReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;
    private final PurchaseEligibilityService purchaseEligibilityService;
    private final ProductClient productClient;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             PurchaseEligibilityService purchaseEligibilityService,
                             ProductClient productClient) {
        this.reviewRepository = reviewRepository;
        this.purchaseEligibilityService = purchaseEligibilityService;
        this.productClient = productClient;
    }

    @Override
    @Transactional
    public ReviewResponse createReview(String userId, CreateReviewRequest request) {
        String normalizedUserId = userId.trim();
        String normalizedProductId = request.getProductId().trim();

        if (reviewRepository.existsByUserIdAndProductId(normalizedUserId, normalizedProductId)) {
            throw new AlreadyReviewedException(normalizedProductId);
        }

        if (!purchaseEligibilityService.hasPurchased(normalizedUserId, normalizedProductId)) {
            throw new ProductNotPurchasedException(normalizedProductId);
        }

        Review review = new Review();
        review.setUserId(normalizedUserId);
        review.setProductId(normalizedProductId);
        review.setRating(request.getRating());
        review.setComment(request.getComment() == null ? null : request.getComment().trim());
        review = reviewRepository.save(review);

        LOGGER.info("Created review id={} userId={} productId={} rating={}",
                review.getId(), normalizedUserId, normalizedProductId, review.getRating());
        return toResponse(review);
    }

    @Override
    @Transactional
    public ReviewResponse updateReview(String reviewId, String userId, com.melodyshop.engagement.dto.UpdateReviewRequest request) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay danh gia"));

        if (!review.getUserId().equals(userId.trim())) {
            throw new IllegalArgumentException("Khong co quyen chinh sua danh gia nay");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment() == null ? null : request.getComment().trim());
        review = reviewRepository.save(review);

        LOGGER.info("Updated review id={} userId={} rating={}", review.getId(), userId, review.getRating());
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductReviewsResponse getReviewsByProduct(String productId) {
        String normalizedProductId = productId.trim();
        List<ReviewResponse> reviews = reviewRepository.findByProductIdOrderByCreatedAtDesc(normalizedProductId)
                .stream()
                .map(this::toResponse)
                .toList();

        long total = reviewRepository.countByProductId(normalizedProductId);
        Double average = reviewRepository.findAverageRatingByProductId(normalizedProductId);
        double roundedAverage = average == null ? 0.0 : Math.round(average * 10.0) / 10.0;

        return new ProductReviewsResponse(normalizedProductId, roundedAverage, total, reviews);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewResponse> getAllReviews(int page, int size) {
        Page<Review> reviewPage = reviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        List<AdminReviewResponse> content = reviewPage.getContent().stream()
                .map(r -> AdminReviewResponse.builder()
                        .id(r.getId())
                        .productId(r.getProductId())
                        .productName(productClient.getProductName(r.getProductId()))
                        .userId(r.getUserId())
                        .userName("Nguoi dung #" + r.getUserId().substring(0, Math.min(6, r.getUserId().length())))
                        .rating(r.getRating())
                        .comment(r.getComment())
                        .createdAt(r.getCreatedAt())
                        .build())
                .toList();

        return PageResponse.<AdminReviewResponse>builder()
                .content(content)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .last(reviewPage.isLast())
                .build();
    }

    @Override
    @Transactional
    public void deleteReview(String reviewId) {
        if (!reviewRepository.existsById(reviewId)) {
            throw new IllegalArgumentException("Khong tim thay danh gia");
        }
        reviewRepository.deleteById(reviewId);
        LOGGER.info("Deleted review id={}", reviewId);
    }

    private ReviewResponse toResponse(Review review) {
        return new ReviewResponse(
                review.getId(),
                review.getUserId(),
                review.getProductId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }
}
