package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.enums.OrderStatus;
import com.melodyshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/quote")
    public ResponseEntity<ApiResponse<CheckoutQuoteDTO>> quoteOrder(
            @Valid @RequestBody CheckoutQuoteRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.quoteOrder(request)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDTO>> createOrder(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDTO order = orderService.createOrder(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable("id") String orderId) {
        OrderDTO order;
        if (userId != null && !userId.isBlank()) {
            order = orderService.getOrderByIdAndUserId(orderId, userId);
        } else {
            order = orderService.getOrderById(orderId);
        }
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrderByNumber(
            @PathVariable("orderNumber") String orderNumber) {
        OrderDTO order = orderService.getOrderByNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> getMyOrders(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderDTO> orders = orderService.getOrdersByUserId(userId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/history/{orderId}")
    public ResponseEntity<ApiResponse<PageResponse<OrderStatusHistoryDTO>>> getOrderHistory(
            @PathVariable("orderId") String orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderStatusHistoryDTO> history = orderService.getOrderStatusHistory(orderId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderDTO>> cancelOrder(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable("id") String orderId,
            @RequestParam(required = false) String reason) {
        OrderDTO order = orderService.cancelOrder(orderId, userId, reason);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đơn hàng thành công", order));
    }

    @GetMapping("/has-orders")
    public ResponseEntity<ApiResponse<Boolean>> hasOrdersByUserId(@RequestParam("userId") String userId) {
        boolean hasOrders = orderService.hasOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.ok(hasOrders));
    }

    @GetMapping("/has-product-orders")
    public ResponseEntity<ApiResponse<Boolean>> hasOrdersByProductId(@RequestParam("productId") String productId) {
        boolean hasOrders = orderService.hasOrdersByProductId(productId);
        return ResponseEntity.ok(ApiResponse.ok(hasOrders));
    }

    @PostMapping("/guest")
    public ResponseEntity<ApiResponse<OrderDTO>> createGuestOrder(
            @Valid @RequestBody GuestCreateOrderRequest request) {
        OrderDTO order = orderService.createGuestOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(order));
    }
}
