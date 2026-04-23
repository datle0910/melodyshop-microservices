package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/api/coupons/validate")
    public ResponseEntity<ApiResponse<CouponDTO>> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.validateCoupon(code, orderAmount)));
    }

    @GetMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponse<PageResponse<CouponDTO>>> getAllCoupons(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CouponDTO> result = couponService.getAllCoupons(PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.<CouponDTO>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .last(result.isLast()).build()));
    }

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponse<CouponDTO>> createCoupon(@Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(couponService.createCoupon(request)));
    }

    @PutMapping("/api/admin/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponDTO>> updateCoupon(
            @PathVariable String id, @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật coupon thành công",
                couponService.updateCoupon(id, request)));
    }
}
