package com.melodyshop.order.repository;

import com.melodyshop.order.entity.OrderStatusLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderStatusLogRepository extends JpaRepository<OrderStatusLog, String> {
    List<OrderStatusLog> findByOrderIdOrderByCreatedAtAsc(String orderId);
}
