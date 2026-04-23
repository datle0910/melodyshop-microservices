package com.melodyshop.order.repository;

import com.melodyshop.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
}
