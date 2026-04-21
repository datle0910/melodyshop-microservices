package com.melodyshop.product.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.product.dto.ReviewDTO;
import com.melodyshop.product.entity.Review;
import com.melodyshop.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductService productService;

    /**
     * Lấy danh sách đánh giá của sản phẩm.
     */
    public Page<ReviewDTO> getReviewsByProductId(String productId, Pageable pageable) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable)
                .map(this::toDTO);
    }

    /**
     * Viết đánh giá sản phẩm.
     */
    @Transactional
    public ReviewDTO createReview(String productId, String userId, ReviewDTO dto) {
        // Kiểm tra user đã đánh giá chưa
        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi");
        }

        Review review = Review.builder()
                .productId(productId)
                .userId(userId)
                .rating(dto.getRating())
                .comment(dto.getComment())
                .isVerified(false)
                .build();

        review = reviewRepository.save(review);

        // Cập nhật avg_rating và review_count trên product
        Double avgRating = reviewRepository.getAverageRating(productId);
        Integer reviewCount = reviewRepository.getReviewCount(productId);
        productService.updateProductRating(productId, avgRating, reviewCount);

        return toDTO(review);
    }

    private ReviewDTO toDTO(Review r) {
        return ReviewDTO.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .userId(r.getUserId())
                .rating(r.getRating())
                .comment(r.getComment())
                .isVerified(r.getIsVerified())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
