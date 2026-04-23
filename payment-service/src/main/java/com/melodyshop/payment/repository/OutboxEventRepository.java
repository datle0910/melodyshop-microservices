package com.melodyshop.payment.repository;

import com.melodyshop.payment.entity.OutboxEvent;
import com.melodyshop.payment.enums.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, String> {
    List<OutboxEvent> findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus status);
}
