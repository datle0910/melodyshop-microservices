package com.melodyshop.order.service.impl;

import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.client.InventoryClient;
import com.melodyshop.order.client.CartClient;
import com.melodyshop.order.client.NotificationClient;
import com.melodyshop.order.client.PaymentClient;
import com.melodyshop.order.client.ProductClient;
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
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
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
    private CartClient cartClient;
    @Mock
    private PaymentClient paymentClient;
    @Mock
    private NotificationClient notificationClient;
    @Mock
    private ProductClient productClient;

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
        createOrderRequest.setExpectedTotal(new BigDecimal("25050000"));

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

        ProductClient.ProductVariantDTO variant = ProductClient.ProductVariantDTO.builder()
                .id("variant-001")
                .variantName("Default")
                .sku("SKU-001")
                .price(new BigDecimal("25000000"))
                .build();
        ProductClient.ProductDTO product = ProductClient.ProductDTO.builder()
                .id("prod-001")
                .name("Fender Stratocaster")
                .basePrice(new BigDecimal("25000000"))
                .variants(List.of(variant))
                .build();
        StockCheckResponse stock = StockCheckResponse.builder()
                .sku("SKU-001")
                .availableQuantity(5)
                .inStock(true)
                .build();
        lenient().when(productClient.getProductById("prod-001")).thenReturn(ApiResponse.ok(product));
        lenient().when(inventoryClient.checkStock(anyString(), anyInt())).thenReturn(ApiResponse.ok(stock));
        lenient().when(inventoryClient.reserveStock(any())).thenReturn(ApiResponse.<Void>ok(null));
        lenient().when(inventoryClient.unreserveStock(any())).thenReturn(ApiResponse.<Void>ok(null));
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
        verify(orderRepository).save(any(Order.class));
        verify(statusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    void quoteOrder_shouldUseAuthoritativeVariantPriceWhenCartSnapshotIsStale() {
        ProductClient.ProductVariantDTO variant = ProductClient.ProductVariantDTO.builder()
                .id("variant-001")
                .variantName("Default")
                .sku("SKU-001")
                .price(new BigDecimal("52000000"))
                .build();
        ProductClient.ProductDTO product = ProductClient.ProductDTO.builder()
                .id("prod-001")
                .name("Yamaha Clavinova CLP-745")
                .basePrice(new BigDecimal("2000"))
                .variants(List.of(variant))
                .build();
        when(productClient.getProductById("prod-001")).thenReturn(ApiResponse.ok(product));
        createOrderRequest.getItems().get(0).setUnitPrice(new BigDecimal("2000"));

        CheckoutQuoteRequest quoteRequest = new CheckoutQuoteRequest();
        quoteRequest.setItems(createOrderRequest.getItems());

        CheckoutQuoteDTO result = orderService.quoteOrder(quoteRequest);

        assertEquals(new BigDecimal("52000000"), result.getSubtotal());
        assertEquals(new BigDecimal("50000.00"), result.getShippingFee());
        assertEquals(new BigDecimal("52050000.00"), result.getTotal());
        assertEquals(new BigDecimal("52000000"), result.getItems().get(0).getUnitPrice());
    }

    @Test
    void quoteOrder_shouldUseUpdatedAuthoritativePrice() {
        ProductClient.ProductVariantDTO variant = ProductClient.ProductVariantDTO.builder()
                .id("variant-001")
                .variantName("Default")
                .sku("SKU-001")
                .price(new BigDecimal("2000"))
                .build();
        ProductClient.ProductDTO product = ProductClient.ProductDTO.builder()
                .id("prod-001")
                .name("Yamaha Clavinova CLP-745")
                .basePrice(new BigDecimal("52000000"))
                .variants(List.of(variant))
                .build();
        when(productClient.getProductById("prod-001")).thenReturn(ApiResponse.ok(product));

        CheckoutQuoteRequest quoteRequest = new CheckoutQuoteRequest();
        quoteRequest.setItems(createOrderRequest.getItems());

        CheckoutQuoteDTO result = orderService.quoteOrder(quoteRequest);

        assertEquals(new BigDecimal("2000"), result.getSubtotal());
        assertEquals(new BigDecimal("52000.00"), result.getTotal());
    }

    @Test
    void quoteOrder_shouldRejectUnsupportedVoucher() {
        CheckoutQuoteRequest quoteRequest = new CheckoutQuoteRequest();
        quoteRequest.setItems(createOrderRequest.getItems());
        quoteRequest.setVoucherCode("SALE10");

        assertThrows(BadRequestException.class, () -> orderService.quoteOrder(quoteRequest));
        verifyNoInteractions(productClient);
    }

    @Test
    void createOrder_shouldIgnoreTamperedClientUnitPrice() {
        createOrderRequest.getItems().get(0).setUnitPrice(BigDecimal.ONE);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-001");
            return o;
        });
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());

        OrderDTO result = orderService.createOrder(userId, createOrderRequest);

        assertEquals(0, new BigDecimal("25000000").compareTo(result.getSubtotal()));
        assertEquals(0, new BigDecimal("25050000").compareTo(result.getTotalAmount()));
        assertEquals(0, new BigDecimal("25000000").compareTo(result.getItems().get(0).getUnitPrice()));
    }

    @Test
    void createOrder_withVietQr_shouldSendAuthoritativeTotalWhenClientUnitPriceIsTampered() {
        createOrderRequest.setPaymentMethod(PaymentMethod.VIETQR);
        createOrderRequest.getItems().get(0).setUnitPrice(BigDecimal.ONE);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-001");
            return o;
        });
        when(statusHistoryRepository.save(any(OrderStatusHistory.class))).thenReturn(new OrderStatusHistory());
        when(paymentClient.createPayment(nullable(String.class), eq(userId), anyString(), any()))
                .thenReturn(ApiResponse.ok(CreatePaymentResponse.builder()
                        .paymentId("payment-001")
                        .amount(new BigDecimal("25050000"))
                        .currency("VND")
                        .build()));

        orderService.createOrder(userId, createOrderRequest);

        ArgumentCaptor<CreatePaymentRequest> paymentRequest = ArgumentCaptor.forClass(CreatePaymentRequest.class);
        verify(paymentClient).createPayment(nullable(String.class), eq(userId), anyString(), paymentRequest.capture());
        assertEquals(0, new BigDecimal("25050000").compareTo(paymentRequest.getValue().getAmount()));
        assertEquals(PaymentMethod.VIETQR, paymentRequest.getValue().getPaymentMethod());
    }

    @Test
    void createOrder_shouldRejectStaleCheckoutQuote() {
        createOrderRequest.setExpectedTotal(new BigDecimal("52000"));

        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(userId, createOrderRequest));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_withOnlinePayment_shouldThrowWhenPaymentFails() {
        createOrderRequest.setPaymentMethod(PaymentMethod.E_WALLET);
        createOrderRequest.getItems().get(0).setSku("SKU-001");

        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            o.setId("order-001");
            return o;
        });
        when(paymentClient.createPayment(nullable(String.class), anyString(), anyString(), any())).thenReturn(null);

        assertThrows(BadRequestException.class, () ->
                orderService.createOrder(userId, createOrderRequest));
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

    @Test
    void handlePaymentSuccess_whenRetried_shouldDeductAndClearCartOnlyOnce() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentId("pay-001");
        order.getItems().get(0).setSku("SKU-001");
        when(orderRepository.findByIdWithItemsForUpdate("order-001")).thenReturn(Optional.of(order));
        when(inventoryClient.deductStock(any())).thenReturn(ApiResponse.<Void>ok(null));
        when(cartClient.clearCart(userId)).thenReturn(ApiResponse.<Void>ok(null));

        orderService.handlePaymentSuccess("order-001", "pay-001");
        orderService.handlePaymentSuccess("order-001", "pay-001");

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertTrue(order.getIsPaid());
        assertTrue(order.getStockDeducted());
        verify(inventoryClient, times(1)).deductStock(any());
        verify(cartClient, times(1)).clearCart(userId);
    }

    @Test
    void handlePaymentSuccess_whenCartClearReturnsError_shouldStillConfirmOrder() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentId("pay-001");
        order.getItems().get(0).setSku("SKU-001");
        when(orderRepository.findByIdWithItemsForUpdate("order-001")).thenReturn(Optional.of(order));
        when(inventoryClient.deductStock(any())).thenReturn(ApiResponse.<Void>ok(null));
        when(cartClient.clearCart(userId)).thenReturn(ApiResponse.error("Cart service temporarily unavailable"));

        assertDoesNotThrow(() -> orderService.handlePaymentSuccess("order-001", "pay-001"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertTrue(order.getIsPaid());
        assertTrue(order.getStockDeducted());
        verify(inventoryClient).deductStock(any());
        verify(cartClient).clearCart(userId);
    }

    @Test
    void handlePaymentSuccess_whenCartClearThrows_shouldStillConfirmOrder() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentId("pay-001");
        order.getItems().get(0).setSku("SKU-001");
        when(orderRepository.findByIdWithItemsForUpdate("order-001")).thenReturn(Optional.of(order));
        when(inventoryClient.deductStock(any())).thenReturn(ApiResponse.<Void>ok(null));
        when(cartClient.clearCart(userId)).thenThrow(new RuntimeException("cart timeout"));

        assertDoesNotThrow(() -> orderService.handlePaymentSuccess("order-001", "pay-001"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertTrue(order.getIsPaid());
        assertTrue(order.getStockDeducted());
        verify(inventoryClient).deductStock(any());
        verify(cartClient).clearCart(userId);
    }

    @Test
    void handlePaymentSuccess_whenUserIdIsEmpty_shouldSkipCartClear() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentId("pay-001");
        order.setUserId(null);
        order.getItems().get(0).setSku("SKU-001");
        when(orderRepository.findByIdWithItemsForUpdate("order-001")).thenReturn(Optional.of(order));
        when(inventoryClient.deductStock(any())).thenReturn(ApiResponse.<Void>ok(null));

        assertDoesNotThrow(() -> orderService.handlePaymentSuccess("order-001", "pay-001"));

        assertEquals(OrderStatus.CONFIRMED, order.getStatus());
        assertTrue(order.getIsPaid());
        assertTrue(order.getStockDeducted());
        verify(inventoryClient).deductStock(any());
        verifyNoInteractions(cartClient);
    }

    @Test
    void handlePaymentFailure_whenRetried_shouldReleaseReservationOnlyOnce() {
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setPaymentId("pay-002");
        order.getItems().get(0).setSku("SKU-001");
        when(orderRepository.findByIdWithItemsForUpdate("order-001")).thenReturn(Optional.of(order));
        when(inventoryClient.unreserveStock(any())).thenReturn(ApiResponse.<Void>ok(null));

        orderService.handlePaymentFailure("order-001", "pay-002", true);
        orderService.handlePaymentFailure("order-001", "pay-002", true);

        assertEquals(OrderStatus.EXPIRED, order.getStatus());
        verify(inventoryClient, times(1)).unreserveStock(any());
    }
}
