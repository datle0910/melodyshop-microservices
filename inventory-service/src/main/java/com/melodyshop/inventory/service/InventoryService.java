package com.melodyshop.inventory.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.inventory.dto.*;
import com.melodyshop.inventory.entity.*;
import com.melodyshop.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private static final String DEFAULT_WAREHOUSE = "default-warehouse-001";

    private final InventoryRepository inventoryRepository;
    private final InventoryLogRepository logRepository;
    private final WarehouseRepository warehouseRepository;

    // ==================== Internal APIs (Order Service gọi) ====================

    /**
     * Kiểm tra tồn kho theo SKU.
     */
    public StockCheckResponse checkStock(String sku, int requestedQuantity) {
        List<Inventory> inventories = inventoryRepository.findBySku(sku);
        int totalAvailable = inventories.stream()
                .mapToInt(Inventory::getAvailableQuantity)
                .sum();

        return StockCheckResponse.builder()
                .sku(sku)
                .availableQuantity(totalAvailable)
                .inStock(totalAvailable >= requestedQuantity)
                .build();
    }

    /**
     * Đặt chỗ (reserve) khi khách tạo đơn — KHÓA KHO.
     * Sử dụng pessimistic lock để tránh race condition.
     */
    @Transactional
    public InventoryDTO reserveStock(InventoryActionRequest request) {
        Inventory inventory = inventoryRepository
                .findBySkuAndWarehouseIdForUpdate(request.getSku(), DEFAULT_WAREHOUSE)
                .orElseThrow(() -> new ResourceNotFoundException("Tồn kho", "sku", request.getSku()));

        int available = inventory.getAvailableQuantity();
        if (available < request.getQuantity()) {
            throw new BadRequestException(
                    String.format("Không đủ hàng. SKU: %s, cần: %d, có sẵn: %d",
                            request.getSku(), request.getQuantity(), available));
        }

        int before = inventory.getReservedQuantity();
        inventory.setReservedQuantity(before + request.getQuantity());
        inventoryRepository.save(inventory);

        // Ghi log
        createLog(inventory, "RESERVE", request.getQuantity(),
                before, inventory.getReservedQuantity(),
                request.getOrderId(), request.getNote(), null);

        log.info("Reserved {} units of SKU {} for order {}",
                request.getQuantity(), request.getSku(), request.getOrderId());

        return toDTO(inventory);
    }

    /**
     * Trừ tồn kho khi đơn được xác nhận xuất hàng.
     */
    @Transactional
    public InventoryDTO deductStock(InventoryActionRequest request) {
        Inventory inventory = inventoryRepository
                .findBySkuAndWarehouseIdForUpdate(request.getSku(), DEFAULT_WAREHOUSE)
                .orElseThrow(() -> new ResourceNotFoundException("Tồn kho", "sku", request.getSku()));

        int beforeQty = inventory.getQuantity();
        int beforeReserved = inventory.getReservedQuantity();

        if (beforeReserved < request.getQuantity()) {
            throw new BadRequestException("Số lượng deduct lớn hơn số lượng đã reserve");
        }

        inventory.setQuantity(beforeQty - request.getQuantity());
        inventory.setReservedQuantity(beforeReserved - request.getQuantity());
        inventoryRepository.save(inventory);

        createLog(inventory, "DEDUCT", -request.getQuantity(),
                beforeQty, inventory.getQuantity(),
                request.getOrderId(), request.getNote(), null);

        log.info("Deducted {} units of SKU {} for order {}",
                request.getQuantity(), request.getSku(), request.getOrderId());

        return toDTO(inventory);
    }

    /**
     * Hủy đặt chỗ khi đơn bị hủy.
     */
    @Transactional
    public InventoryDTO unreserveStock(InventoryActionRequest request) {
        Inventory inventory = inventoryRepository
                .findBySkuAndWarehouseIdForUpdate(request.getSku(), DEFAULT_WAREHOUSE)
                .orElseThrow(() -> new ResourceNotFoundException("Tồn kho", "sku", request.getSku()));

        int before = inventory.getReservedQuantity();
        int unreserveQty = Math.min(request.getQuantity(), before);

        inventory.setReservedQuantity(before - unreserveQty);
        inventoryRepository.save(inventory);

        createLog(inventory, "UNRESERVE", -unreserveQty,
                before, inventory.getReservedQuantity(),
                request.getOrderId(), request.getNote(), null);

        log.info("Unreserved {} units of SKU {} for order {}",
                unreserveQty, request.getSku(), request.getOrderId());

        return toDTO(inventory);
    }

    // ==================== Admin APIs ====================

    /**
     * Danh sách tồn kho (Admin).
     */
    public Page<InventoryDTO> getAllInventory(Pageable pageable) {
        return inventoryRepository.findAll(pageable).map(this::toDTO);
    }

    /**
     * Cập nhật tồn kho (nhập hàng) — Admin.
     */
    @Transactional
    public InventoryDTO updateInventory(String id, UpdateInventoryRequest request) {
        Inventory inventory = inventoryRepository.findById(id).orElse(null);

        if (inventory == null) {
            // Tạo mới nếu chưa có
            inventory = Inventory.builder()
                    .productId(request.getProductId())
                    .variantId(request.getVariantId())
                    .sku(request.getSku())
                    .warehouseId(request.getWarehouseId())
                    .quantity(request.getQuantity())
                    .reservedQuantity(0)
                    .reorderPoint(request.getReorderPoint() != null ? request.getReorderPoint() : 10)
                    .build();
            inventory = inventoryRepository.save(inventory);

            createLog(inventory, "IMPORT", request.getQuantity(),
                    0, request.getQuantity(),
                    null, request.getNote(), null);
        } else {
            int before = inventory.getQuantity();
            int diff = request.getQuantity() - before;
            inventory.setQuantity(request.getQuantity());
            if (request.getReorderPoint() != null) {
                inventory.setReorderPoint(request.getReorderPoint());
            }
            inventory = inventoryRepository.save(inventory);

            createLog(inventory, "ADJUST", diff,
                    before, request.getQuantity(),
                    null, request.getNote(), null);
        }

        return toDTO(inventory);
    }

    /**
     * Danh sách sắp hết hàng (Admin).
     */
    public Page<InventoryDTO> getLowStock(Pageable pageable) {
        return inventoryRepository.findLowStock(pageable).map(this::toDTO);
    }

    /**
     * Lịch sử biến động kho (Admin).
     */
    public Page<InventoryLogDTO> getInventoryLogs(String inventoryId, Pageable pageable) {
        return logRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId, pageable)
                .map(this::toLogDTO);
    }

    // ==================== Warehouse CRUD ====================

    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseRepository.findByIsActiveTrueOrderByNameAsc()
                .stream().map(this::toWarehouseDTO).collect(Collectors.toList());
    }

    @Transactional
    public WarehouseDTO createWarehouse(WarehouseDTO dto) {
        Warehouse warehouse = Warehouse.builder()
                .name(dto.getName())
                .location(dto.getLocation())
                .isActive(true)
                .build();
        warehouse = warehouseRepository.save(warehouse);
        return toWarehouseDTO(warehouse);
    }

    // ==================== Private helpers ====================

    private void createLog(Inventory inventory, String action, int change,
                           int before, int after, String refId, String note, String userId) {
        InventoryLog log = InventoryLog.builder()
                .inventoryId(inventory.getId())
                .action(action)
                .quantityChange(change)
                .quantityBefore(before)
                .quantityAfter(after)
                .referenceId(refId)
                .note(note)
                .createdBy(userId)
                .build();
        logRepository.save(log);
    }

    private InventoryDTO toDTO(Inventory i) {
        String warehouseName = warehouseRepository.findById(i.getWarehouseId())
                .map(Warehouse::getName).orElse(null);

        return InventoryDTO.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .variantId(i.getVariantId())
                .sku(i.getSku())
                .warehouseId(i.getWarehouseId())
                .warehouseName(warehouseName)
                .quantity(i.getQuantity())
                .reservedQuantity(i.getReservedQuantity())
                .availableQuantity(i.getAvailableQuantity())
                .reorderPoint(i.getReorderPoint())
                .lowStock(i.getAvailableQuantity() <= i.getReorderPoint())
                .createdAt(i.getCreatedAt())
                .updatedAt(i.getUpdatedAt())
                .build();
    }

    private InventoryLogDTO toLogDTO(InventoryLog l) {
        return InventoryLogDTO.builder()
                .id(l.getId())
                .inventoryId(l.getInventoryId())
                .action(l.getAction())
                .quantityChange(l.getQuantityChange())
                .quantityBefore(l.getQuantityBefore())
                .quantityAfter(l.getQuantityAfter())
                .referenceId(l.getReferenceId())
                .note(l.getNote())
                .createdBy(l.getCreatedBy())
                .createdAt(l.getCreatedAt())
                .build();
    }

    private WarehouseDTO toWarehouseDTO(Warehouse w) {
        return WarehouseDTO.builder()
                .id(w.getId())
                .name(w.getName())
                .location(w.getLocation())
                .isActive(w.getIsActive())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }
}
