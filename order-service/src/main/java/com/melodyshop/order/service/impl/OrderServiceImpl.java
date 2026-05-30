package com.melodyshop.order.service.impl;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.client.CartClient;
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
import com.melodyshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository statusHistoryRepository;
    private final InventoryClient inventoryClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final CartClient cartClient;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("50000.00");

    @Override
    @Transactional
    public OrderDTO createOrder(String userId, CreateOrderRequest request) {
        Order order = Order.builder()
                .userId(userId)
                .shippingFullName(request.getShippingFullName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity())
                .shippingEmail(request.getShippingEmail())
                .shippingPostalCode(request.getShippingPostalCode())
                .orderNote(request.getOrderNote())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .shippingFee(DEFAULT_SHIPPING_FEE)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .isPaid(false)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            // Check stock availability before creating order
            if (itemReq.getSku() != null && !itemReq.getSku().isBlank()) {
                try {
                    ApiResponse<StockCheckResponse> checkResp = inventoryClient.checkStock(
                            itemReq.getSku(), itemReq.getQuantity());
                    if (checkResp != null && checkResp.getData() != null
                            && !checkResp.getData().getInStock()) {
                        throw new BadRequestException(String.format(
                                "Sản phẩm \"%s\" đã hết hàng hoặc không đủ số lượng (SKU: %s). Vui lòng giảm số lượng hoặc chọn sản phẩm khác.",
                                itemReq.getProductName(), itemReq.getSku()));
                    }
                } catch (BadRequestException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("Stock check failed for SKU {}: {}", itemReq.getSku(), e.getMessage());
                }
            }

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productImage(itemReq.getProductImage())
                    .variantId(itemReq.getVariantId())
                    .variantName(itemReq.getVariantName())
                    .sku(itemReq.getSku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();
            item.setOrder(order);
            orderItems.add(item);

            subtotal = subtotal.add(itemTotal);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal.add(DEFAULT_SHIPPING_FEE));

        order = orderRepository.save(order);

        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            // DEMO: Simulate successful payment for Credit Card
            order.setIsPaid(true);
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setStockDeducted(true);
            order.setPaymentId("DUMMY_CC_" + System.currentTimeMillis());
            orderRepository.save(order);
            deductInventory(order);
        } else if (request.getPaymentMethod() != PaymentMethod.COD) {
            try {
                CreatePaymentRequest paymentReq = new CreatePaymentRequest();
                paymentReq.setOrderId(order.getId());
                paymentReq.setOrderNumber(order.getOrderNumber());
                paymentReq.setAmount(order.getTotalAmount());
                paymentReq.setPaymentMethod(request.getPaymentMethod());
                paymentReq.setProvider(request.getProvider());
                paymentReq.setCurrency("VND");
                ApiResponse<CreatePaymentResponse> paymentResp = paymentClient.createPayment(paymentReq);
                if (paymentResp != null && paymentResp.isSuccess() && paymentResp.getData() != null) {
                    order.setPaymentId(paymentResp.getData().getPaymentId());
                    order.setPaymentUrl(paymentResp.getData().getRedirectUrl());
                    orderRepository.save(order);
                } else {
                    String msg = paymentResp != null ? paymentResp.getMessage() : "Tạo thanh toán thất bại";
                    log.error("Failed to create payment for order {}: {}", order.getOrderNumber(), msg);
                    throw new BadRequestException("Không thể tạo thanh toán: " + msg);
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to create payment for order {}: {}", order.getOrderNumber(), e.getMessage());
                throw new BadRequestException("Không thể tạo thanh toán. Vui lòng thử lại sau.");
            }
        }

        createStatusHistory(order.getId(), null, OrderStatus.PENDING.name(), "Don hang duoc tao", userId);

        // Clear the user's cart after successful order creation
        try {
            cartClient.clearCart(userId);
            log.info("Cart cleared for user {} after order {}", userId, order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to clear cart for user {}: {}", userId, e.getMessage());
        }

        // Send order confirmation email asynchronously (after order is committed)
        sendOrderConfirmationEmailAsync(order);

        // Reserve stock
        reserveInventory(order);

        log.info("Created order {} for user {}", order.getOrderNumber(), userId);
        return toDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO createGuestOrder(GuestCreateOrderRequest request) {
        Order order = Order.builder()
                .userId(null)
                .shippingFullName(request.getShippingFullName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity() != null ? request.getShippingCity() : "")
                .shippingEmail(request.getShippingEmail())
                .shippingPostalCode(request.getShippingPostalCode())
                .orderNote(request.getOrderNote())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .shippingFee(DEFAULT_SHIPPING_FEE)
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .isPaid(false)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            // Check stock availability before creating guest order
            if (itemReq.getSku() != null && !itemReq.getSku().isBlank()) {
                try {
                    ApiResponse<StockCheckResponse> checkResp = inventoryClient.checkStock(
                            itemReq.getSku(), itemReq.getQuantity());
                    if (checkResp != null && checkResp.getData() != null
                            && !checkResp.getData().getInStock()) {
                        throw new BadRequestException(String.format(
                                "Sản phẩm \"%s\" đã hết hàng hoặc không đủ số lượng (SKU: %s). Vui lòng giảm số lượng hoặc chọn sản phẩm khác.",
                                itemReq.getProductName(), itemReq.getSku()));
                    }
                } catch (BadRequestException e) {
                    throw e;
                } catch (Exception e) {
                    log.warn("Stock check failed for SKU {}: {}", itemReq.getSku(), e.getMessage());
                }
            }

            BigDecimal itemTotal = itemReq.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(itemReq.getProductName())
                    .productImage(itemReq.getProductImage())
                    .variantId(itemReq.getVariantId())
                    .variantName(itemReq.getVariantName())
                    .sku(itemReq.getSku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(itemReq.getUnitPrice())
                    .totalPrice(itemTotal)
                    .build();
            item.setOrder(order);
            orderItems.add(item);
            subtotal = subtotal.add(itemTotal);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal.add(DEFAULT_SHIPPING_FEE));

        order = orderRepository.save(order);

        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            // DEMO: Simulate successful payment for Credit Card
            order.setIsPaid(true);
            order.setPaidAt(LocalDateTime.now());
            order.setStatus(OrderStatus.CONFIRMED);
            order.setStockDeducted(true);
            order.setPaymentId("DUMMY_CC_GUEST_" + System.currentTimeMillis());
            orderRepository.save(order);
            deductInventory(order);
        } else if (request.getPaymentMethod() != PaymentMethod.COD) {
            try {
                CreatePaymentRequest paymentReq = new CreatePaymentRequest();
                paymentReq.setOrderId(order.getId());
                paymentReq.setOrderNumber(order.getOrderNumber());
                paymentReq.setAmount(order.getTotalAmount());
                paymentReq.setPaymentMethod(request.getPaymentMethod());
                ApiResponse<CreatePaymentResponse> paymentResp = paymentClient.createPayment(paymentReq);
                if (paymentResp != null && paymentResp.isSuccess() && paymentResp.getData() != null) {
                    order.setPaymentId(paymentResp.getData().getPaymentId());
                    order.setPaymentUrl(paymentResp.getData().getRedirectUrl());
                    orderRepository.save(order);
                }
            } catch (BadRequestException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to create payment for guest order {}: {}", order.getOrderNumber(), e.getMessage());
                throw new BadRequestException("Khong the tao thanh toan. Vui long thu lai sau.");
            }
        }

        createStatusHistory(order.getId(), null, OrderStatus.PENDING.name(), "Don hang duoc tao (khach vang lai)", "GUEST");

        // Send order confirmation email asynchronously (after order is committed)
        sendOrderConfirmationEmailAsync(order);

        log.info("Created guest order {}", order.getOrderNumber());
        return toDTO(order);
    }

    @Override
    public OrderDTO getOrderById(String orderId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return toDTO(order);
    }

    @Override
    public OrderDTO getOrderByIdAndUserId(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserIdWithItems(orderId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        return toDTO(order);
    }

    @Override
    public OrderDTO getOrderByNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return toDTO(order);
    }

    @Override
    public PageResponse<OrderDTO> getOrdersByUserId(String userId, Pageable pageable) {
        Page<Order> page = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<OrderDTO> getAllOrders(Pageable pageable) {
        Page<Order> page = orderRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPageResponse(page);
    }

    @Override
    public PageResponse<OrderDTO> getOrdersByStatus(OrderStatus status, Pageable pageable) {
        Page<Order> page = orderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        return toPageResponse(page);
    }

    @Override
    @Transactional
    public OrderDTO updateOrderStatus(String orderId, String changedBy, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        OrderStatus oldStatus = order.getStatus();
        OrderStatus newStatus = request.getStatus();

        validateStatusTransition(oldStatus, newStatus);

        order.setStatus(newStatus);
        order = orderRepository.save(order);

        createStatusHistory(orderId, oldStatus.name(), newStatus.name(), request.getNote(), changedBy);

        handleStatusSideEffects(order, oldStatus, newStatus);

        log.info("Order {} status changed from {} to {} by {}",
                order.getOrderNumber(), oldStatus, newStatus, changedBy);
        return toDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO cancelOrder(String orderId, String userId, String reason) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Ban khong co quyen huy don hang nay");
        }

        if (order.getStatus() == OrderStatus.DELIVERED ||
            order.getStatus() == OrderStatus.CANCELLED ||
            order.getStatus() == OrderStatus.REFUNDED) {
            throw new BadRequestException("Khong the huy don hang o trang thai hien tai");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Hoàn lại stock nếu đã bị trừ
        if (Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreInventory(order);
            order.setStockDeducted(false);
            orderRepository.save(order);
        }

        createStatusHistory(orderId, oldStatus.name(), OrderStatus.CANCELLED.name(), reason, userId);

        log.info("Order {} cancelled by user {}", order.getOrderNumber(), userId);
        return toDTO(order);
    }

    @Override
    public PageResponse<OrderStatusHistoryDTO> getOrderStatusHistory(String orderId, Pageable pageable) {
        Page<OrderStatusHistory> page = statusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId, pageable);
        return PageResponse.<OrderStatusHistoryDTO>builder()
                .content(page.getContent().stream().map(this::toHistoryDTO).collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    @Transactional
    public void handlePaymentSuccess(String orderId, String paymentId) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        order.setIsPaid(true);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // TRỪ stock ngay khi xác nhận đơn hàng (đặt hàng thành công)
        deductInventory(order);
        order.setStockDeducted(true);
        orderRepository.save(order);
        createStatusHistory(orderId, OrderStatus.PENDING.name(), OrderStatus.CONFIRMED.name(), "Thanh toan thanh cong", null);

        try {
            String customerEmail = order.getShippingEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                log.warn("No shipping email for order {}, skipping notification", order.getOrderNumber());
            } else {
                notificationClient.sendOrderConfirmation(
                        customerEmail,
                        order.getOrderNumber(),
                        order.getTotalAmount().toString(),
                        order.getShippingFullName());
            }
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }

        log.info("Payment success for order {}, inventory reserved", order.getOrderNumber());
    }

    private void sendOrderConfirmationEmailAsync(Order order) {
        try {
            String customerEmail = order.getShippingEmail();
            if (customerEmail == null || customerEmail.isBlank()) {
                log.warn("No shipping email for order {}, skipping notification", order.getOrderNumber());
                return;
            }
            notificationClient.sendOrderConfirmation(
                    customerEmail,
                    order.getOrderNumber(),
                    order.getTotalAmount().toString(),
                    order.getShippingFullName());
            log.info("Order confirmation email sent for order {}", order.getOrderNumber());
        } catch (Exception e) {
            log.error("Failed to send order confirmation email for {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    @Override
    @Transactional
    public OrderItemDTO updateOrderItemQuantity(String orderId, String itemId, int newQuantity) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Khong the cap nhat san pham khi don hang da hoan thanh hoac da huy");
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "id", itemId));

        BigDecimal oldTotal = item.getTotalPrice();
        item.setQuantity(newQuantity);
        item.setTotalPrice(item.getUnitPrice().multiply(BigDecimal.valueOf(newQuantity)));
        orderItemRepository.save(item);

        // Recalculate order totals
        BigDecimal newSubtotal = order.getItems().stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        order.setSubtotal(newSubtotal);
        order.setTotalAmount(newSubtotal.add(order.getShippingFee()).subtract(order.getDiscountAmount()));
        orderRepository.save(order);

        log.info("Order item {} quantity updated from {} to {} for order {}",
                itemId, oldTotal, item.getTotalPrice(), order.getOrderNumber());
        return toItemDTO(item);
    }

    @Override
    public boolean hasOrdersByUserId(String userId) {
        return orderRepository.existsByUserId(userId);
    }

    @Override
    public boolean hasOrdersByProductId(String productId) {
        return orderItemRepository.existsByProductId(productId);
    }

    private void validateStatusTransition(OrderStatus from, OrderStatus to) {
        boolean valid = switch (to) {
            case CONFIRMED -> from == OrderStatus.PENDING || from == OrderStatus.PROCESSING;
            case PROCESSING -> from == OrderStatus.CONFIRMED;
            case SHIPPED -> from == OrderStatus.PROCESSING;
            case DELIVERED -> from == OrderStatus.SHIPPED;
            case CANCELLED -> from != OrderStatus.DELIVERED && from != OrderStatus.CANCELLED && from != OrderStatus.REFUNDED;
            case REFUNDED -> from == OrderStatus.DELIVERED;
            case PENDING -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Khong the chuyen tu trang thai '%s' sang '%s'", from, to));
        }
    }

    private void handleStatusSideEffects(Order order, OrderStatus oldStatus, OrderStatus newStatus) {
        // CONFIRMED: stock đã được trừ trong handlePaymentSuccess (khi thanh toán online)
        // Hoặc trừ ở đây nếu admin xác nhận đơn COD (chưa thanh toán online)
        if (newStatus == OrderStatus.CONFIRMED && oldStatus == OrderStatus.PENDING) {
            if (!Boolean.TRUE.equals(order.getStockDeducted())) {
                deductInventory(order);
                order.setStockDeducted(true);
                orderRepository.save(order);
            }
        }

        // CANCELLED: hoàn lại stock nếu đã trừ
        if (newStatus == OrderStatus.CANCELLED) {
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreInventory(order);
                order.setStockDeducted(false);
                orderRepository.save(order);
            }
        }

        // REFUNDED: hoàn lại stock (khách đã nhận hàng nhưng được hoàn tiền)
        if (newStatus == OrderStatus.REFUNDED) {
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreInventory(order);
                order.setStockDeducted(false);
                orderRepository.save(order);
            }
        }
    }

    private void reserveInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            try {
                InventoryActionRequest req = new InventoryActionRequest();
                req.setSku(item.getSku());
                req.setQuantity(item.getQuantity());
                req.setOrderId(order.getId());
                req.setNote("Reserve for order " + order.getOrderNumber());
                inventoryClient.reserveStock(req);
            } catch (Exception e) {
                log.error("Failed to reserve inventory for SKU {}: {}",
                        item.getSku(), e.getMessage());
            }
        }
    }

    private void unreserveInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            try {
                InventoryActionRequest req = new InventoryActionRequest();
                req.setSku(item.getSku());
                req.setQuantity(item.getQuantity());
                req.setOrderId(order.getId());
                req.setNote("Unreserve for cancelled order " + order.getOrderNumber());
                inventoryClient.unreserveStock(req);
            } catch (Exception e) {
                log.error("Failed to unreserve inventory for SKU {}: {}",
                        item.getSku(), e.getMessage());
            }
        }
    }

    private void deductInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            try {
                InventoryActionRequest req = new InventoryActionRequest();
                req.setSku(item.getSku());
                req.setQuantity(item.getQuantity());
                req.setOrderId(order.getId());
                req.setNote("Deduct for order " + order.getOrderNumber());
                inventoryClient.deductStock(req);
            } catch (Exception e) {
                log.error("Failed to deduct inventory for SKU {}: {}",
                        item.getSku(), e.getMessage());
            }
        }
    }

    private void restoreInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            try {
                InventoryActionRequest req = new InventoryActionRequest();
                req.setSku(item.getSku());
                req.setQuantity(item.getQuantity());
                req.setOrderId(order.getId());
                req.setNote("Restore stock for cancelled/refunded order " + order.getOrderNumber());
                inventoryClient.restoreStock(req);
            } catch (Exception e) {
                log.error("Failed to restore inventory for SKU {}: {}",
                        item.getSku(), e.getMessage());
            }
        }
    }

    private void createStatusHistory(String orderId, String from, String to, String note, String changedBy) {
        OrderStatusHistory history = OrderStatusHistory.builder()
                .orderId(orderId)
                .fromStatus(from)
                .toStatus(to)
                .note(note)
                .changedBy(changedBy)
                .build();
        statusHistoryRepository.save(history);
    }

    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> itemDTOs = order.getItems() == null
                ? new ArrayList<>()
                : order.getItems().stream().map(this::toItemDTO).collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .userId(order.getUserId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .shippingFullName(order.getShippingFullName())
                .shippingPhone(order.getShippingPhone())
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingEmail(order.getShippingEmail())
                .shippingPostalCode(order.getShippingPostalCode())
                .orderNote(order.getOrderNote())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .taxAmount(order.getTaxAmount())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .paymentId(order.getPaymentId())
                .paymentUrl(order.getPaymentUrl())
                .isPaid(order.getIsPaid())
                .paidAt(order.getPaidAt())
                .stockDeducted(order.getStockDeducted())
                .items(itemDTOs)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(item.getProductName())
                .productImage(item.getProductImage())
                .variantId(item.getVariantId())
                .variantName(item.getVariantName())
                .sku(item.getSku())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .build();
    }

    private OrderStatusHistoryDTO toHistoryDTO(OrderStatusHistory h) {
        return OrderStatusHistoryDTO.builder()
                .id(h.getId())
                .orderId(h.getOrderId())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .note(h.getNote())
                .changedBy(h.getChangedBy())
                .createdAt(h.getCreatedAt())
                .build();
    }

    private PageResponse<OrderDTO> toPageResponse(Page<Order> page) {
        return PageResponse.<OrderDTO>builder()
                .content(page.getContent().stream().map(this::toDTO).collect(Collectors.toList()))
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
    @Override
    @Transactional
    public OrderDTO updatePaymentStatus(String orderId, String changedBy) {
        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        if (order.getIsPaid()) {
            throw new BadRequestException("Đơn hàng này đã được thanh toán");
        }

        order.setIsPaid(true);
        order.setPaidAt(LocalDateTime.now());
        
        order = orderRepository.save(order);

        createStatusHistory(orderId, order.getStatus().name(), order.getStatus().name(), "Cập nhật thanh toán thủ công", changedBy);

        log.info("Order {} payment status marked as paid by {}", order.getOrderNumber(), changedBy);
        return toDTO(order);
    }
}
