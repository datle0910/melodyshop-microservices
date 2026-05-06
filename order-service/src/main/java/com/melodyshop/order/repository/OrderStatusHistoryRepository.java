package com.melodyshop.order.repository;

import com.melodyshop.order.entity.OrderStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, String> {
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(String orderId);
    Page<OrderStatusHistory> findByOrderIdOrderByCreatedAtDesc(String orderId, Pageable pageable);
}
