package com.melodyshop.engagement.repository;

import com.melodyshop.engagement.entity.PurchasedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PurchasedProductRepository extends JpaRepository<PurchasedProduct, String> {
    boolean existsByUserIdAndProductId(String userId, String productId);
    Optional<PurchasedProduct> findByUserIdAndProductId(String userId, String productId);
}
