package com.melodyshop.engagement.repository;

import com.melodyshop.engagement.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, String> {
    boolean existsByUserIdAndProductId(String userId, String productId);
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);
    long countByProductId(String productId);

    @Query("select avg(r.rating) from Review r where r.productId = :productId")
    Double findAverageRatingByProductId(@Param("productId") String productId);

    // Admin methods - paginated listing
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
