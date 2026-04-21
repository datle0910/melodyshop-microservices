package com.melodyshop.product.repository;

import com.melodyshop.product.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    Page<Review> findByProductIdOrderByCreatedAtDesc(String productId, Pageable pageable);
    Optional<Review> findByUserIdAndProductId(String userId, String productId);
    boolean existsByUserIdAndProductId(String userId, String productId);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.productId = :productId")
    Double getAverageRating(@Param("productId") String productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :productId")
    Integer getReviewCount(@Param("productId") String productId);
}
