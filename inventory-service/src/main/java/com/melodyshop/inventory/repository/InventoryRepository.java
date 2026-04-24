package com.melodyshop.inventory.repository;

import com.melodyshop.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, String> {

    Optional<Inventory> findBySkuAndWarehouseId(String sku, String warehouseId);
    boolean existsBySkuAndWarehouseId(String sku, String warehouseId);

    /**
     * Lấy tồn kho theo SKU (tổng từ tất cả kho).
     */
    List<Inventory> findBySku(String sku);

    /**
     * Pessimistic lock khi reserve/deduct — tránh race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.sku = :sku AND i.warehouseId = :warehouseId")
    Optional<Inventory> findBySkuAndWarehouseIdForUpdate(
            @Param("sku") String sku,
            @Param("warehouseId") String warehouseId);

    /**
     * Lấy danh sách sản phẩm sắp hết hàng.
     */
    @Query("SELECT i FROM Inventory i WHERE (i.quantity - i.reservedQuantity) <= i.reorderPoint")
    Page<Inventory> findLowStock(Pageable pageable);

    Page<Inventory> findAll(Pageable pageable);

    List<Inventory> findByProductId(String productId);
}
