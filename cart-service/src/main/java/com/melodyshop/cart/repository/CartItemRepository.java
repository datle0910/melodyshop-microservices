package com.melodyshop.cart.repository;

import com.melodyshop.cart.entity.Cart;
import com.melodyshop.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, String> {

    List<CartItem> findByCartIdOrderByCreatedAtAsc(String cartId);

    Optional<CartItem> findByCartIdAndProductIdAndVariantId(String cartId, String productId, String variantId);

    Optional<CartItem> findByCartIdAndSku(String cartId, String sku);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.id = :itemId")
    void deleteByCartIdAndId(@Param("cartId") String cartId, @Param("itemId") String itemId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") String cartId);

    int countByCartId(String cartId);
}
