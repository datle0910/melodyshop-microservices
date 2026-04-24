package com.melodyshop.inventory.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.inventory.dto.InventoryActionRequest;
import com.melodyshop.inventory.dto.InventoryDTO;
import com.melodyshop.inventory.dto.StockCheckResponse;
import com.melodyshop.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Internal API — được gọi bởi Order Service qua Feign.
 * Tất cả endpoint yêu cầu Bearer Token (validated at Gateway).
 */
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Kiểm tra tồn kho (cho Order Service gọi) — Internal
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<StockCheckResponse>> checkStock(
            @RequestParam String sku,
            @RequestParam(defaultValue = "1") int quantity) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.checkStock(sku, quantity)));
    }

    /**
     * Đặt chỗ (reserve) khi tạo đơn — Internal
     * Sử dụng Pessimistic Lock để tránh tranh chấp hàng.
     */
    @PutMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryDTO>> reserveStock(
            @Valid @RequestBody InventoryActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Đặt chỗ thành công",
                inventoryService.reserveStock(request)));
    }

    /**
     * Trừ tồn kho khi xuất hàng — Internal
     */
    @PutMapping("/deduct")
    public ResponseEntity<ApiResponse<InventoryDTO>> deductStock(
            @Valid @RequestBody InventoryActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Trừ tồn kho thành công",
                inventoryService.deductStock(request)));
    }

    /**
     * Hủy đặt chỗ khi đơn bị hủy — Internal
     */
    @PutMapping("/unreserve")
    public ResponseEntity<ApiResponse<InventoryDTO>> unreserveStock(
            @Valid @RequestBody InventoryActionRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Hủy đặt chỗ thành công",
                inventoryService.unreserveStock(request)));
    }

    /**
     * Khởi tạo kho cho sản phẩm mới — Internal call từ Product Service.
     */
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<Void>> initInventory(
            @RequestParam String productId,
            @RequestParam(required = false) String variantId,
            @RequestParam String sku) {
        inventoryService.initInventory(productId, variantId, sku);
        return ResponseEntity.ok(ApiResponse.ok("Khởi tạo kho thành công", null));
    }
}
