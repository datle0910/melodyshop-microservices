package com.melodyshop.inventory.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.inventory.dto.*;
import com.melodyshop.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin API — yêu cầu Bearer Token + ADMIN role.
 */
@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

    private final InventoryService inventoryService;

    /**
     * Danh sách tồn kho — ADMIN (Bearer Token required)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<InventoryDTO>>> getAllInventory(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InventoryDTO> result = inventoryService.getAllInventory(PageRequest.of(page, size));

        PageResponse<InventoryDTO> pageResponse = PageResponse.<InventoryDTO>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    /**
     * Cập nhật tồn kho (nhập hàng) — ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InventoryDTO>> updateInventory(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật tồn kho thành công",
                inventoryService.updateInventory(id, request)));
    }

    /**
     * Cảnh báo sắp hết hàng — ADMIN
     */
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<PageResponse<InventoryDTO>>> getLowStock(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InventoryDTO> result = inventoryService.getLowStock(PageRequest.of(page, size));

        PageResponse<InventoryDTO> pageResponse = PageResponse.<InventoryDTO>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    /**
     * Lịch sử biến động kho — ADMIN
     */
    @GetMapping("/{id}/logs")
    public ResponseEntity<ApiResponse<PageResponse<InventoryLogDTO>>> getInventoryLogs(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<InventoryLogDTO> result = inventoryService.getInventoryLogs(id, PageRequest.of(page, size));

        PageResponse<InventoryLogDTO> pageResponse = PageResponse.<InventoryLogDTO>builder()
                .content(result.getContent())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();

        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    // ==================== Warehouses ====================

    /**
     * Danh sách kho — ADMIN
     */
    @GetMapping("/warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getWarehouses(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllWarehouses()));
    }

    /**
     * Thêm kho — ADMIN
     */
    @PostMapping("/warehouses")
    public ResponseEntity<ApiResponse<WarehouseDTO>> createWarehouse(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody WarehouseDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(inventoryService.createWarehouse(dto)));
    }
}
