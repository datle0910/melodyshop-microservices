package com.melodyshop.order.repository;

import com.melodyshop.order.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    List<CartItem> findByCartId(String cartId);
    void deleteByCartId(String cartId);
}
