package com.melodyshop.order.repository;

import com.melodyshop.order.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, String> {
    List<Wishlist> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Wishlist> findByUserIdAndProductId(String userId, String productId);
    boolean existsByUserIdAndProductId(String userId, String productId);
    void deleteByUserIdAndProductId(String userId, String productId);
}
