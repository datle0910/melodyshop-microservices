package com.melodyshop.order.service.impl;

import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.client.InventoryClient;
import com.melodyshop.order.client.NotificationClient;
import com.melodyshop.order.client.PaymentClient;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.entity.Order;
import com.melodyshop.order.entity.OrderItem;
import com.melodyshop.order.entity.OrderStatusHistory;
import com.melodyshop.order.enums.OrderStatus;
import com.melodyshop.order.enums.PaymentMethod;
import com.melodyshop.order.repository.OrderItemRepository;
import com.melodyshop.order.repository.OrderRepository;
import com.melodyshop.order.repository.OrderStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderStatusHistoryRepository statusHistoryRepository;
    @Mock
    private InventoryClient inventoryClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private OrderServiceImpl orderService;

    private String userId;
    private CreateOrderRequest createOrderRequest;
    private Order order;

    @BeforeEach
    void setUp() {
        userId = "user-001";

        OrderItemRequest itemReq = new OrderItemRequest();
        itemReq.setProductId("prod-001");
        itemReq.setProductName("Fender Stratocaster");
        itemReq.setQuantity(1);
        itemReq.setUnitPrice(new BigDecimal("25000000"));

        createOrderRequest = new CreateOrderRequest();
        createOrderRequest.setShippingFullName("Nguyen Van A");
        createOrderRequest.setShippingPhone("0909123456");
        createOrderRequest.setShippingAddress("123 Duong ABC, Quan 1");
        createOrderRequest.setPaymentMethod(PaymentMethod.COD);
        createOrderRequest.setItems(List.of(itemReq));

        OrderItem orderItem = OrderItem.builder()
                .productId("prod-001")
                .productName("Fender Stratocaster")
                .quantity(1)
                .unitPrice(new BigDecimal("25000000"))
                .totalPrice(new BigDecimal("25000000"))
                .build();

        order = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.PENDING)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC, Quan 1")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .isPaid(false)
                .items(new ArrayList<>(List.of(orderItem)))
                .build();
        order.setId("order-001");
        orderItem.setOrder(order);
    }

    @Test
    void createOrder_shouldCreateOrderSuccessfully() {
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-001");
            return o;
        });
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        OrderDTO result = orderService.createOrder(userId, createOrderRequest);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
        assertEquals(OrderStatus.PENDING, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals("Fender Stratocaster", result.getItems().get(0).getProductName());
        verify(orderRepository, times(2)).save(any(Order.class));
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void createOrder_withOnlinePayment_shouldCreatePayment() {
        createOrderRequest.setPaymentMethod(PaymentMethod.CREDIT_CARD);
        createOrderRequest.getItems().get(0).setSku("SKU-001");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-001");
            return o;
        });
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(paymentClient.createPayment(any())).thenReturn(null);

        OrderDTO result = orderService.createOrder(userId, createOrderRequest);

        assertNotNull(result);
        verify(paymentClient).createPayment(any());
    }

    @Test
    void getOrderById_shouldReturnOrder() {
        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderById("order-001");

        assertNotNull(result);
        assertEquals("order-001", result.getId());
        assertEquals("MS123456", result.getOrderNumber());
    }

    @Test
    void getOrderById_notFound_shouldThrowException() {
        when(orderRepository.findByIdWithItems("nonexistent")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                orderService.getOrderById("nonexistent"));
    }

    @Test
    void getOrderByIdAndUserId_shouldReturnOrder() {
        when(orderRepository.findByIdAndUserIdWithItems("order-001", userId)).thenReturn(Optional.of(order));

        OrderDTO result = orderService.getOrderByIdAndUserId("order-001", userId);

        assertNotNull(result);
        assertEquals(userId, result.getUserId());
    }

    @Test
    void getOrdersByUserId_shouldReturnPaginatedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)).thenReturn(page);

        PageResponse<OrderDTO> result = orderService.getOrdersByUserId(userId, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void updateOrderStatus_confirmPending_shouldSucceed() {
        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.CONFIRMED);
        req.setNote("Xac nhan don hang");

        Order pendingOrder = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.PENDING)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .build();
        pendingOrder.setId("order-001");

        Order confirmedOrder = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.CONFIRMED)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .build();
        confirmedOrder.setId("order-001");

        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(pendingOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);
        when(statusHistoryRepository.save(any())).thenReturn(new OrderStatusHistory());

        OrderDTO result = orderService.updateOrderStatus("order-001", "admin-001", req);

        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void updateOrderStatus_invalidTransition_shouldThrowException() {
        Order deliveredOrder = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.DELIVERED)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .build();
        deliveredOrder.setId("order-001");

        UpdateOrderStatusRequest req = new UpdateOrderStatusRequest();
        req.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(deliveredOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.updateOrderStatus("order-001", "admin-001", req));
    }

    @Test
    void cancelOrder_byOwner_shouldSucceed() {
        Order confirmedOrder = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.CONFIRMED)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .build();
        confirmedOrder.setId("order-001");

        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(confirmedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(confirmedOrder);
        when(statusHistoryRepository.save(any())).thenReturn(new OrderStatusHistory());

        OrderDTO result = orderService.cancelOrder("order-001", userId, "Khong can nua");

        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
    }

    @Test
    void cancelOrder_byNonOwner_shouldThrowException() {
        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("order-001", "other-user", "reason"));
    }

    @Test
    void cancelOrder_alreadyDelivered_shouldThrowException() {
        Order deliveredOrder = Order.builder()
                .userId(userId)
                .orderNumber("MS123456")
                .status(OrderStatus.DELIVERED)
                .shippingFullName("Nguyen Van A")
                .shippingPhone("0909123456")
                .shippingAddress("123 Duong ABC")
                .subtotal(new BigDecimal("25000000"))
                .shippingFee(new BigDecimal("50000"))
                .totalAmount(new BigDecimal("25050000"))
                .paymentMethod(PaymentMethod.COD)
                .items(new ArrayList<>())
                .build();
        deliveredOrder.setId("order-001");

        when(orderRepository.findByIdWithItems("order-001")).thenReturn(Optional.of(deliveredOrder));

        assertThrows(BadRequestException.class, () ->
                orderService.cancelOrder("order-001", userId, "reason"));
    }

    @Test
    void getAllOrders_shouldReturnPaginatedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(page);

        PageResponse<OrderDTO> result = orderService.getAllOrders(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getOrdersByStatus_shouldReturnFilteredOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findByStatusOrderByCreatedAtDesc(OrderStatus.PENDING, pageable)).thenReturn(page);

        PageResponse<OrderDTO> result = orderService.getOrdersByStatus(OrderStatus.PENDING, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(OrderStatus.PENDING, result.getContent().get(0).getStatus());
    }
}
