package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> getAllOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderDTO> result = orderService.getAdminOrders(status, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.<OrderDTO>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .last(result.isLast()).build()));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateStatus(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable String id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
                orderService.updateOrderStatus(adminId, id, request)));
    }
}
