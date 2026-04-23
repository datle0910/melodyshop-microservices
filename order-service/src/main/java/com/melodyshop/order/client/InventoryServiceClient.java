package com.melodyshop.order.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

/**
 * Feign client để gọi Inventory Service.
 * Dùng cho check stock, reserve, unreserve khi tạo/hủy đơn hàng.
 */
@FeignClient(name = "inventory-service")
public interface InventoryServiceClient {

    @GetMapping("/api/inventory/check")
    ApiResponse<Map<String, Object>> checkStock(
            @RequestParam("sku") String sku,
            @RequestParam("quantity") int quantity);

    @PutMapping("/api/inventory/reserve")
    ApiResponse<Map<String, Object>> reserveStock(@RequestBody Map<String, Object> request);

    @PutMapping("/api/inventory/unreserve")
    ApiResponse<Map<String, Object>> unreserveStock(@RequestBody Map<String, Object> request);
}
