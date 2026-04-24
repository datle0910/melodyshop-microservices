package com.melodyshop.product.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/api/inventory/init")
    ApiResponse<Void> initInventory(
            @RequestParam("productId") String productId,
            @RequestParam(value = "variantId", required = false) String variantId,
            @RequestParam("sku") String sku
    );
}
