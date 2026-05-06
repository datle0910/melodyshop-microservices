package com.melodyshop.order.client.fallback;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.client.InventoryClient;
import com.melodyshop.order.dto.InventoryActionRequest;
import com.melodyshop.order.dto.StockCheckResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryClientFallback implements InventoryClient {

    @Override
    public ApiResponse<StockCheckResponse> checkStock(String sku, int requestedQuantity) {
        log.warn("Fallback activated for checkStock - sku: {}, requestedQuantity: {}", sku, requestedQuantity);
        StockCheckResponse fallbackResponse = StockCheckResponse.builder()
                .sku(sku)
                .availableQuantity(0)
                .inStock(false)
                .build();
        return ApiResponse.<StockCheckResponse>builder()
                .success(false)
                .message("Inventory service temporarily unavailable")
                .data(fallbackResponse)
                .build();
    }

    @Override
    public ApiResponse<Void> reserveStock(InventoryActionRequest request) {
        log.warn("Fallback activated for reserveStock - sku: {}", request != null ? request.getSku() : "null");
        return ApiResponse.error("Inventory service temporarily unavailable");
    }

    @Override
    public ApiResponse<Void> deductStock(InventoryActionRequest request) {
        log.warn("Fallback activated for deductStock - sku: {}", request != null ? request.getSku() : "null");
        return ApiResponse.error("Inventory service temporarily unavailable");
    }

    @Override
    public ApiResponse<Void> unreserveStock(InventoryActionRequest request) {
        log.warn("Fallback activated for unreserveStock - sku: {}", request != null ? request.getSku() : "null");
        return ApiResponse.error("Inventory service temporarily unavailable");
    }
}
