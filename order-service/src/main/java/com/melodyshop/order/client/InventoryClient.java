package com.melodyshop.order.client;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.fallback.InventoryClientFallbackFactory;
import com.melodyshop.order.dto.InventoryActionRequest;
import com.melodyshop.order.dto.StockCheckResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "inventory-service", fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient {

    @GetMapping("/api/inventory/check")
    ApiResponse<StockCheckResponse> checkStock(
            @RequestParam("sku") String sku,
            @RequestParam("quantity") int requestedQuantity);

    @PutMapping("/api/inventory/reserve")
    ApiResponse<Void> reserveStock(@RequestBody InventoryActionRequest request);

    @PutMapping("/api/inventory/deduct")
    ApiResponse<Void> deductStock(@RequestBody InventoryActionRequest request);

    @PutMapping("/api/inventory/unreserve")
    ApiResponse<Void> unreserveStock(@RequestBody InventoryActionRequest request);

    @PutMapping("/api/inventory/restore")
    ApiResponse<Void> restoreStock(@RequestBody InventoryActionRequest request);
}
