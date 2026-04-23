package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(orderService.createOrder(userId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<OrderDTO> result = orderService.getMyOrders(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.<OrderDTO>builder()
                .content(result.getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages())
                .last(result.isLast()).build()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            @RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderById(userId, id)));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<ApiResponse<List<OrderStatusLogDTO>>> getTimeline(
            @RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrderTimeline(userId, id)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @RequestHeader("X-User-Id") String userId, @PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Đã hủy đơn hàng", orderService.cancelOrder(userId, id)));
    }
}
