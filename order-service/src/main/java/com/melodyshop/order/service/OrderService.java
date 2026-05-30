package com.melodyshop.order.service;

import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.enums.OrderStatus;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    OrderDTO createOrder(String userId, CreateOrderRequest request);
    OrderDTO createGuestOrder(GuestCreateOrderRequest request);
    OrderDTO getOrderById(String orderId);
    OrderDTO getOrderByIdAndUserId(String orderId, String userId);
    OrderDTO getOrderByNumber(String orderNumber);
    PageResponse<OrderDTO> getOrdersByUserId(String userId, Pageable pageable);
    PageResponse<OrderDTO> getAllOrders(Pageable pageable);
    PageResponse<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable);
    OrderDTO updateOrderStatus(String orderId, String changedBy, UpdateOrderStatusRequest request);
    OrderDTO cancelOrder(String orderId, String userId, String reason);
    PageResponse<OrderStatusHistoryDTO> getOrderStatusHistory(String orderId, Pageable pageable);
    OrderItemDTO updateOrderItemQuantity(String orderId, String itemId, int newQuantity);
    boolean hasOrdersByUserId(String userId);
    boolean hasOrdersByProductId(String productId);
    OrderDTO updatePaymentStatus(String orderId, String changedBy);
}
