package com.melodyshop.inventory.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.inventory.dto.*;
import com.melodyshop.inventory.entity.*;
import com.melodyshop.inventory.client.ProductClient;
import com.melodyshop.inventory.repository.*;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final InventoryImportRepository importRepository;
    private final InventoryImportItemRepository importItemRepository;
    private final ProductClient productClient;

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

        if (alreadyProcessed(inventory, "RESERVE", request.getOrderId())) {
            return toDTO(inventory);
        }
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

        if (alreadyProcessed(inventory, "DEDUCT", request.getOrderId())) {
            return toDTO(inventory);
        }
        int beforeQty = inventory.getQuantity();
        int beforeReserved = inventory.getReservedQuantity();

        if (beforeQty < request.getQuantity() || beforeReserved < request.getQuantity()) {
            throw new BadRequestException(
                    String.format("Cannot deduct unreserved stock. SKU: %s, requested: %d, quantity: %d, reserved: %d",
                            request.getSku(), request.getQuantity(), beforeQty, beforeReserved));
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

        if (alreadyProcessed(inventory, "UNRESERVE", request.getOrderId())) {
            return toDTO(inventory);
        }
        int before = inventory.getReservedQuantity();
        if (before < request.getQuantity()) {
            throw new BadRequestException(
                    String.format("Cannot release missing reservation. SKU: %s, requested: %d, reserved: %d",
                            request.getSku(), request.getQuantity(), before));
        }
        int unreserveQty = request.getQuantity();

        inventory.setReservedQuantity(before - unreserveQty);
        inventoryRepository.save(inventory);

        createLog(inventory, "UNRESERVE", -unreserveQty,
                before, inventory.getReservedQuantity(),
                request.getOrderId(), request.getNote(), null);

        log.info("Unreserved {} units of SKU {} for order {}",
                unreserveQty, request.getSku(), request.getOrderId());

        return toDTO(inventory);
    }

    /**
     * Hoàn lại stock khi đơn bị hủy hoặc hoàn tiền.
     * Tăng cả quantity (tổng tồn kho) và bỏ reserve.
     */
    @Transactional
    public InventoryDTO restoreStock(InventoryActionRequest request) {
        Inventory inventory = inventoryRepository
                .findBySkuAndWarehouseIdForUpdate(request.getSku(), DEFAULT_WAREHOUSE)
                .orElseThrow(() -> new ResourceNotFoundException("Tồn kho", "sku", request.getSku()));

        if (alreadyProcessed(inventory, "RESTORE", request.getOrderId())) {
            return toDTO(inventory);
        }
        int beforeQty = inventory.getQuantity();
        int beforeReserved = inventory.getReservedQuantity();

        // Hoàn lại: tăng quantity, giảm reserved
        inventory.setQuantity(beforeQty + request.getQuantity());
        inventory.setReservedQuantity(Math.max(0, beforeReserved - request.getQuantity()));
        inventoryRepository.save(inventory);

        createLog(inventory, "RESTORE", request.getQuantity(),
                beforeQty, inventory.getQuantity(),
                request.getOrderId(), request.getNote(), null);

        log.info("Restored {} units of SKU {} for order {}",
                request.getQuantity(), request.getSku(), request.getOrderId());

        return toDTO(inventory);
    }

    /**
     * Khởi tạo kho cho sản phẩm mới (số lượng 0).
     */
    @Transactional
    public void initInventory(String productId, String variantId, String sku) {
        if (inventoryRepository.existsBySkuAndWarehouseId(sku, DEFAULT_WAREHOUSE)) {
            return;
        }

        Inventory inventory = Inventory.builder()
                .productId(productId)
                .variantId(variantId)
                .sku(sku)
                .warehouseId(DEFAULT_WAREHOUSE)
                .quantity(0)
                .reservedQuantity(0)
                .reorderPoint(10)
                .build();
        inventoryRepository.save(inventory);
        log.info("Initialized inventory for SKU: {} with 0 items", sku);
    }

    /**
     * Lấy thông tin tồn kho theo SKU (dùng cho product-service hiển thị stock).
     */
    public StockInfoResponse getStockInfo(String sku) {
        List<Inventory> inventories = inventoryRepository.findBySku(sku);
        if (inventories.isEmpty()) {
            return StockInfoResponse.builder()
                    .sku(sku)
                    .quantity(0)
                    .reservedQuantity(0)
                    .availableQuantity(0)
                    .lowStock(true)
                    .build();
        }

        int totalQty = inventories.stream().mapToInt(Inventory::getQuantity).sum();
        int totalReserved = inventories.stream().mapToInt(Inventory::getReservedQuantity).sum();
        int totalAvailable = totalQty - totalReserved;

        // Check low stock across all warehouse records for this SKU
        boolean lowStock = inventories.stream()
                .anyMatch(inv -> inv.getAvailableQuantity() <= inv.getReorderPoint());

        return StockInfoResponse.builder()
                .sku(sku)
                .quantity(totalQty)
                .reservedQuantity(totalReserved)
                .availableQuantity(totalAvailable)
                .lowStock(lowStock)
                .build();
    }

    // ==================== Admin APIs ====================

    /**
     * Nhập hàng vào kho — tạo phiếu nhập + cập nhật tồn kho.
     */
    @Transactional
    public ImportDTO importStock(ImportRequest request, String userId) {
        String importCode = generateImportCode();

        InventoryImport importRecord = InventoryImport.builder()
                .importCode(importCode)
                .note(request.getNote())
                .importedBy(userId)
                .totalQuantity(0)
                .build();

        int totalQtyAdded = 0;

        for (ImportRequest.ImportItemRequest itemReq : request.getItems()) {
            Inventory inventory = inventoryRepository
                    .findBySkuAndWarehouseId(itemReq.getSku(), DEFAULT_WAREHOUSE)
                    .orElse(null);

            int quantityBefore;
            int quantityAfter;

            if (inventory == null) {
                // Tạo mới inventory record nếu chưa có
                inventory = Inventory.builder()
                        .productId(itemReq.getProductId())
                        .variantId(itemReq.getVariantId())
                        .sku(itemReq.getSku())
                        .warehouseId(DEFAULT_WAREHOUSE)
                        .quantity(0)
                        .reservedQuantity(0)
                        .reorderPoint(10)
                        .build();
                quantityBefore = 0;
            } else {
                quantityBefore = inventory.getQuantity();
            }

            inventory.setQuantity(quantityBefore + itemReq.getQuantity());
            quantityAfter = inventory.getQuantity();
            inventoryRepository.save(inventory);

            // Ghi log nhập hàng
            createLog(inventory, "IMPORT", itemReq.getQuantity(),
                    quantityBefore, quantityAfter,
                    null, request.getNote(), userId);

            // Lấy giá từ ProductService để tính giá nhập = 80% giá bán
            BigDecimal importPrice = BigDecimal.ZERO;
            try {
                var response = productClient.getProductById(itemReq.getProductId());
                if (response != null && response.isSuccess() && response.getData() != null) {
                    BigDecimal sellPrice = response.getData().getPrice();
                    if (sellPrice != null) {
                        importPrice = sellPrice.multiply(new BigDecimal("0.8"));
                    }
                }
            } catch (Exception e) {
                log.error("Failed to get product price for ID {}: {}", itemReq.getProductId(), e.getMessage());
            }

            // Tạo item cho phiếu nhập
            InventoryImportItem importItem = InventoryImportItem.builder()
                    .sku(itemReq.getSku())
                    .productId(itemReq.getProductId())
                    .variantId(itemReq.getVariantId())
                    .productName(itemReq.getProductName())
                    .quantityBefore(quantityBefore)
                    .quantityAfter(quantityAfter)
                    .quantityAdded(itemReq.getQuantity())
                    .importPrice(importPrice)
                    .build();

            importRecord.addItem(importItem);
            totalQtyAdded += itemReq.getQuantity();
        }

        importRecord.setTotalQuantity(totalQtyAdded);
        importRepository.save(importRecord);

        log.info("Import stock completed: code={}, items={}, totalQty={}, by={}",
                importCode, request.getItems().size(), totalQtyAdded, userId);

        return toImportDTO(importRecord);
    }

    /**
     * Lấy danh sách phiếu nhập hàng (Admin).
     */
    public List<ImportDTO> getImportHistory() {
        return importRepository.findAll().stream()
                .map(this::toImportDTO)
                .collect(Collectors.toList());
    }

    private String generateImportCode() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String code = "IMP" + timestamp;
        int suffix = 1;
        while (importRepository.existsByImportCode(code + (suffix > 1 ? "-" + suffix : ""))) {
            suffix++;
        }
        return suffix > 1 ? code + "-" + suffix : code;
    }

    private ImportDTO toImportDTO(InventoryImport imp) {
        List<ImportDTO.ImportItemDTO> itemDTOs = imp.getItems().stream()
                .map(item -> ImportDTO.ImportItemDTO.builder()
                        .id(item.getId())
                        .sku(item.getSku())
                        .productId(item.getProductId())
                        .variantId(item.getVariantId())
                        .productName(item.getProductName())
                        .quantityBefore(item.getQuantityBefore())
                        .quantityAfter(item.getQuantityAfter())
                        .quantityAdded(item.getQuantityAdded())
                        .importPrice(item.getImportPrice())
                        .build())
                .collect(Collectors.toList());

        return ImportDTO.builder()
                .id(imp.getId())
                .importCode(imp.getImportCode())
                .note(imp.getNote())
                .importedBy(imp.getImportedBy())
                .totalQuantity(imp.getTotalQuantity())
                .items(itemDTOs)
                .createdAt(imp.getCreatedAt())
                .build();
    }

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

    private boolean alreadyProcessed(Inventory inventory, String action, String referenceId) {
        return referenceId != null
                && logRepository.existsByInventoryIdAndActionAndReferenceId(inventory.getId(), action, referenceId);
    }

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

        String productName = null;
        try {
            var response = productClient.getProductById(i.getProductId());
            if (response != null && response.isSuccess() && response.getData() != null) {
                productName = response.getData().getName();
            }
        } catch (Exception e) {
            log.error("Failed to fetch product name for ID {}: {}", i.getProductId(), e.getMessage());
        }

        return InventoryDTO.builder()
                .id(i.getId())
                .productId(i.getProductId())
                .variantId(i.getVariantId())
                .sku(i.getSku())
                .warehouseId(i.getWarehouseId())
                .warehouseName(warehouseName)
                .productName(productName)
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
