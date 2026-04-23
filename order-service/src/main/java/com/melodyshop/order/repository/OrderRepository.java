package com.melodyshop.order.repository;

import com.melodyshop.order.entity.Order;
import com.melodyshop.order.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllOrders(Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderCode LIKE :prefix%")
    long countByOrderCodePrefix(String prefix);
}
