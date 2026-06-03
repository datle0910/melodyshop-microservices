package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * REST-based inventory client using RestTemplate with LoadBalancer.
 */
@Component
public class InventoryRestClient {

    @LoadBalanced
    @Autowired
    private RestTemplate restTemplate;

    private static final String BASE_URL = "http://INVENTORY-SERVICE";

    public record StockCheckDTO(
            Boolean inStock,
            Integer availableQuantity,
            Integer reservedQuantity
    ) {
        public boolean isInStock() { return Boolean.TRUE.equals(inStock); }
    }

    public ApiResponse<StockCheckDTO> checkStock(String sku, Integer quantity) {
        return restTemplate.exchange(
            BASE_URL + "/api/inventory/check?sku=" + sku + "&quantity=" + quantity,
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<ApiResponse<StockCheckDTO>>() {}
        ).getBody();
    }
}
