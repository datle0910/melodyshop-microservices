package com.melodyshop.search.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.search.dto.ProductIndexRequest;
import com.melodyshop.search.dto.ProductSearchResult;
import com.melodyshop.search.service.ProductSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService searchService;

    /**
     * Tìm kiếm sản phẩm siêu tốc — Public
     * Hỗ trợ: keyword (tên, mô tả), category, brand, giá, sắp xếp.
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductSearchResult>>> searchProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "relevance") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        List<ProductSearchResult> results = searchService.search(
                keyword, categoryId, brandId, minPrice, maxPrice,
                sortBy, sortDir, page, size);

        return ResponseEntity.ok(ApiResponse.ok(results));
    }

    /**
     * Gợi ý tìm kiếm (autocomplete) — Public
     */
    @GetMapping("/suggest")
    public ResponseEntity<ApiResponse<List<String>>> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(searchService.suggest(q, limit)));
    }

    // ==================== Index Management (Internal — Product Service gọi) ====================

    /**
     * Đồng bộ 1 sản phẩm vào index — Internal (Bearer Token required)
     */
    @PostMapping("/index")
    public ResponseEntity<ApiResponse<Void>> indexProduct(
            @Valid @RequestBody ProductIndexRequest request) {
        searchService.indexProduct(request);
        return ResponseEntity.ok(ApiResponse.ok("Đã đồng bộ sản phẩm vào index", null));
    }

    /**
     * Đồng bộ nhiều sản phẩm (bulk) — Internal
     */
    @PostMapping("/index/bulk")
    public ResponseEntity<ApiResponse<Void>> indexProducts(
            @Valid @RequestBody List<ProductIndexRequest> requests) {
        searchService.indexProducts(requests);
        return ResponseEntity.ok(ApiResponse.ok("Đã đồng bộ " + requests.size() + " sản phẩm vào index", null));
    }

    /**
     * Xóa sản phẩm khỏi index — Internal
     */
    @DeleteMapping("/index/{id}")
    public ResponseEntity<Void> deleteFromIndex(@PathVariable String id) {
        searchService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}
