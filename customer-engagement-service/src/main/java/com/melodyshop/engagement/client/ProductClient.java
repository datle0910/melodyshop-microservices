package com.melodyshop.engagement.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductClient {

    private final RestTemplate restTemplate;

    @Autowired
    public ProductClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getProductName(String productId) {
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> response = restTemplate.getForObject(
                    "http://product-service/api/products/" + productId,
                    java.util.Map.class
            );
            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.get("data");
                return (String) data.get("name");
            }
        } catch (Exception e) {
            // Product service may not be available, return fallback
        }
        return "San pham #" + productId.substring(0, Math.min(8, productId.length()));
    }
}
