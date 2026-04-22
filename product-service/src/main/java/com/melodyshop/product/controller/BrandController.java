package com.melodyshop.product.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.product.dto.BrandDTO;
import com.melodyshop.product.service.BrandService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    /**
     * Lấy danh sách thương hiệu — Public
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<BrandDTO>>> getBrands() {
        return ResponseEntity.ok(ApiResponse.ok(brandService.getAllBrands()));
    }

    /**
     * Chi tiết thương hiệu — Public
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDTO>> getBrandById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(brandService.getBrandById(id)));
    }

    /**
     * Thêm thương hiệu — ADMIN (Bearer Token required)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BrandDTO>> createBrand(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @Valid @RequestBody BrandDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(brandService.createBrand(dto)));
    }

    /**
     * Sửa thương hiệu — ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<BrandDTO>> updateBrand(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id,
            @Valid @RequestBody BrandDTO dto) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thương hiệu thành công",
                brandService.updateBrand(id, dto)));
    }

    /**
     * Xóa thương hiệu (soft delete) — ADMIN
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBrand(
            @RequestHeader("X-User-Id") String userId,
            @RequestHeader("X-User-Role") String role,
            @PathVariable String id) {
        brandService.deleteBrand(id);
        return ResponseEntity.ok(ApiResponse.ok("Xóa thương hiệu thành công", null));
    }
}
