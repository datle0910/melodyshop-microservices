package com.melodyshop.ai.infrastructure.client;

import com.melodyshop.common.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "PRODUCT-SERVICE")
public interface CategoryClient {

    @GetMapping("/api/categories")
    ApiResponse<List<CategoryDTO>> getCategories();

    record CategoryDTO(
            String id,
            String name,
            String slug,
            String description,
            String imageUrl,
            String parentId,
            Integer sortOrder,
            Boolean isActive,
            List<CategoryDTO> children
    ) {}
}
