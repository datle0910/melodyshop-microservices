package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "INVENTORY-SERVICE")
public interface InventoryClient {

    @GetMapping("/api/inventory/check")
    ApiResponse<StockCheckDTO> checkStock(
            @RequestParam("sku") String sku,
            @RequestParam("quantity") Integer quantity
    );

    record StockCheckDTO(
            Boolean inStock,
            Integer availableQuantity,
            Integer reservedQuantity
    ) {}
}
