package com.melodyshop.engagement.repository;

import com.melodyshop.engagement.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, String> {
    Optional<WishlistItem> findByUserIdAndProductId(String userId, String productId);
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(String userId);
}
