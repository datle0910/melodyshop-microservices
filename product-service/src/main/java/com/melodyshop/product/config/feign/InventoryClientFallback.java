package com.melodyshop.product.config.feign;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.product.client.InventoryClient;
import com.melodyshop.product.dto.StockInfoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryClientFallback implements InventoryClient {

    @Override
    public ApiResponse<Void> initInventory(String productId, String variantId, String sku) {
        log.warn("Fallback activated: Inventory service temporarily unavailable for productId={}, sku={}", productId, sku);
        return ApiResponse.error("Inventory service temporarily unavailable");
    }

    @Override
    public ApiResponse<StockInfoResponse> getStockInfo(String sku) {
        log.warn("Fallback: getStockInfo for sku={} — returning null stock info", sku);
        StockInfoResponse fallbackResponse = StockInfoResponse.builder()
                .sku(sku)
                .quantity(null)
                .reservedQuantity(null)
                .availableQuantity(null)
                .lowStock(false)
                .build();
        return ApiResponse.<StockInfoResponse>builder()
                .success(true)
                .data(fallbackResponse)
                .build();
    }
}
