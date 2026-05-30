package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.OrderDTO;
import com.melodyshop.order.dto.OrderItemDTO;
import com.melodyshop.order.dto.OrderStatusHistoryDTO;
import com.melodyshop.order.dto.UpdateOrderItemRequest;
import com.melodyshop.order.dto.UpdateOrderStatusRequest;
import com.melodyshop.order.enums.OrderStatus;
import com.melodyshop.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDTO>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) OrderStatus status) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderDTO> orders;
        if (status != null) {
            orders = orderService.getOrdersByStatus(status, pageable);
        } else {
            orders = orderService.getAllOrders(pageable);
        }
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDTO>> getOrder(@PathVariable("id") String orderId) {
        OrderDTO order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderDTO>> updateOrderStatus(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable("id") String orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        OrderDTO order = orderService.updateOrderStatus(orderId, adminId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công", order));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<PageResponse<OrderStatusHistoryDTO>>> getOrderHistory(
            @PathVariable("id") String orderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<OrderStatusHistoryDTO> history = orderService.getOrderStatusHistory(orderId, pageable);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    public ResponseEntity<ApiResponse<OrderItemDTO>> updateOrderItemQuantity(
            @PathVariable("orderId") String orderId,
            @PathVariable("itemId") String itemId,
            @Valid @RequestBody UpdateOrderItemRequest request) {
        OrderItemDTO item = orderService.updateOrderItemQuantity(orderId, itemId, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok("Cap nhat so luong thanh cong", item));
    }

    @PutMapping("/{id}/paid")
    public ResponseEntity<ApiResponse<OrderDTO>> updatePaymentStatus(
            @RequestHeader("X-User-Id") String adminId,
            @PathVariable("id") String orderId) {
        OrderDTO order = orderService.updatePaymentStatus(orderId, adminId);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thanh toán thành công", order));
    }
}
