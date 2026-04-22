package com.melodyshop.inventory.repository;

import com.melodyshop.inventory.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, String> {
    Page<InventoryLog> findByInventoryIdOrderByCreatedAtDesc(String inventoryId, Pageable pageable);
}
