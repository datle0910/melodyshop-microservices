package com.melodyshop.order.service.impl;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.common.dto.PageResponse;
import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.client.CartClient;
import com.melodyshop.order.client.InventoryClient;
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
import com.melodyshop.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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
    private final ProductClient productClient;

    private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("50000.00");

    @Value("${internal.service-token}")
    private String internalServiceToken;

    @Override
    @Transactional(readOnly = true)
    public CheckoutQuoteDTO quoteOrder(CheckoutQuoteRequest request) {
        requireVoucherSupport(request.getVoucherCode());
        return buildCheckoutQuote(request.getItems());
    }

    @Override
    @Transactional
    public OrderDTO createOrder(String userId, CreateOrderRequest request) {
        if (request.getPaymentMethod() == null) {
            throw new BadRequestException("Phương thức thanh toán không được để trống");
        }
        if (request.getPaymentMethod() == PaymentMethod.CREDIT_CARD) {
            throw new BadRequestException("Thanh toán thẻ trực tiếp đã được tắt. Vui lòng chọn VietQR hoặc COD.");
        }
        OrderStatus initialStatus = request.getPaymentMethod() == PaymentMethod.VIETQR
                ? OrderStatus.PENDING_PAYMENT
                : OrderStatus.PENDING;
        requireVoucherSupport(request.getVoucherCode());
        CheckoutQuoteDTO quote = buildCheckoutQuote(request.getItems());
        requireExpectedTotalMatches(request.getExpectedTotal(), quote.getTotal());
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
                .status(initialStatus)
                .shippingFee(quote.getShippingFee())
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(quote.getDiscount())
                .isPaid(false)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            // Auto-heal missing variantId and SKU from product-service
            if (itemReq.getSku() == null || itemReq.getSku().isBlank()) {
                try {
                    ApiResponse<ProductClient.ProductDTO> prodResp = productClient.getProductById(itemReq.getProductId());
                    if (prodResp != null && prodResp.isSuccess() && prodResp.getData() != null) {
                        ProductClient.ProductDTO productData = prodResp.getData();
                        if (productData.getVariants() != null && !productData.getVariants().isEmpty()) {
                            ProductClient.ProductVariantDTO variant = productData.getVariants().stream()
                                    .filter(v -> "Mặc định".equalsIgnoreCase(v.getVariantName()))
                                    .findFirst()
                                    .orElse(productData.getVariants().get(0));
                            itemReq.setVariantId(variant.getId());
                            itemReq.setVariantName(variant.getVariantName());
                            itemReq.setSku(variant.getSku());
                            log.info("Auto-healed missing SKU for product {}: SKU={}, variantId={}", 
                                    itemReq.getProductName(), variant.getSku(), variant.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-heal missing SKU for product ID {}: {}", itemReq.getProductId(), e.getMessage());
                }
            }

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

            ResolvedProductItem resolved = resolveProductItem(itemReq);
            requireStockAvailable(resolved.sku(), itemReq.getQuantity(), resolved.productName());
            BigDecimal itemTotal = resolved.unitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(resolved.productName())
                    .productImage(itemReq.getProductImage())
                    .variantId(resolved.variantId())
                    .variantName(resolved.variantName())
                    .sku(resolved.sku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(resolved.unitPrice())
                    .totalPrice(itemTotal)
                    .build();
            item.setOrder(order);
            orderItems.add(item);

            subtotal = subtotal.add(itemTotal);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal.add(DEFAULT_SHIPPING_FEE));
        requireExpectedTotalMatches(quote.getTotal(), order.getTotalAmount());

        order = orderRepository.save(order);
        CreatePaymentResponse paymentDetails = null;
        try {
            reserveInventory(order);
        } catch (Exception ex) {
            compensateReservation(order);
            throw new BadRequestException("Không thể giữ tồn kho cho đơn hàng: " + ex.getMessage());
        }

        if (request.getPaymentMethod() != PaymentMethod.COD) {
            try {
                CreatePaymentRequest paymentReq = new CreatePaymentRequest();
                paymentReq.setOrderId(order.getId());
                paymentReq.setOrderNumber(order.getOrderNumber());
                paymentReq.setAmount(order.getTotalAmount());
                paymentReq.setPaymentMethod(request.getPaymentMethod());
                paymentReq.setProvider(request.getPaymentMethod() == PaymentMethod.VIETQR ? "VIETQR" : request.getProvider());
                paymentReq.setCurrency("VND");
                ApiResponse<CreatePaymentResponse> paymentResp = paymentClient.createPayment(
                        internalServiceToken, userId, "order-" + order.getId(), paymentReq);
                if (paymentResp != null && paymentResp.isSuccess() && paymentResp.getData() != null) {
                    paymentDetails = paymentResp.getData();
                    order.setPaymentId(paymentDetails.getPaymentId());
                    order.setPaymentUrl(paymentDetails.getRedirectUrl());
                    order = orderRepository.save(order);
                } else {
                    String msg = paymentResp != null ? paymentResp.getMessage() : "Tạo thanh toán thất bại";
                    log.error("Failed to create payment for order {}: {}", order.getOrderNumber(), msg);
                    throw new BadRequestException("Không thể tạo thanh toán: " + msg);
                }
            } catch (BadRequestException e) {
                compensateReservation(order);
                throw e;
            } catch (Exception e) {
                log.error("Failed to create payment for order {}: {}", order.getOrderNumber(), e.getMessage());
                compensateReservation(order);
                throw new BadRequestException("Không thể tạo thanh toán. Vui lòng thử lại sau.");
            }
        }

        createStatusHistory(order.getId(), null, initialStatus.name(), "Don hang duoc tao", userId);
        if (request.getPaymentMethod() == PaymentMethod.COD) {
            sendOrderConfirmationEmailAsync(order);
        }

        log.info("Created order {} for user {}", order.getOrderNumber(), userId);
        return enrichPaymentDetails(toDTO(order), paymentDetails);
    }

    @Override
    @Transactional
    public OrderDTO createGuestOrder(GuestCreateOrderRequest request) {
        if (request.getPaymentMethod() != PaymentMethod.COD) {
            throw new BadRequestException("Khách vãng lai hiện chỉ hỗ trợ COD. Vui lòng đăng nhập để thanh toán VietQR.");
        }
        requireVoucherSupport(request.getVoucherCode());
        CheckoutQuoteDTO quote = buildCheckoutQuote(request.getItems());
        requireExpectedTotalMatches(request.getExpectedTotal(), quote.getTotal());
        Order order = Order.builder()
                .userId(UUID.randomUUID().toString())
                .shippingFullName(request.getShippingFullName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddress(request.getShippingAddress())
                .shippingCity(request.getShippingCity() != null ? request.getShippingCity() : "")
                .shippingEmail(request.getShippingEmail())
                .shippingPostalCode(request.getShippingPostalCode())
                .orderNote(request.getOrderNote())
                .paymentMethod(request.getPaymentMethod())
                .status(OrderStatus.PENDING)
                .shippingFee(quote.getShippingFee())
                .taxAmount(BigDecimal.ZERO)
                .discountAmount(quote.getDiscount())
                .isPaid(false)
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            // Auto-heal missing variantId and SKU from product-service
            if (itemReq.getSku() == null || itemReq.getSku().isBlank()) {
                try {
                    ApiResponse<ProductClient.ProductDTO> prodResp = productClient.getProductById(itemReq.getProductId());
                    if (prodResp != null && prodResp.isSuccess() && prodResp.getData() != null) {
                        ProductClient.ProductDTO productData = prodResp.getData();
                        if (productData.getVariants() != null && !productData.getVariants().isEmpty()) {
                            ProductClient.ProductVariantDTO variant = productData.getVariants().stream()
                                    .filter(v -> "Mặc định".equalsIgnoreCase(v.getVariantName()))
                                    .findFirst()
                                    .orElse(productData.getVariants().get(0));
                            itemReq.setVariantId(variant.getId());
                            itemReq.setVariantName(variant.getVariantName());
                            itemReq.setSku(variant.getSku());
                            log.info("Auto-healed missing SKU for guest product {}: SKU={}, variantId={}", 
                                    itemReq.getProductName(), variant.getSku(), variant.getId());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to auto-heal missing SKU for guest product ID {}: {}", itemReq.getProductId(), e.getMessage());
                }
            }

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

            ResolvedProductItem resolved = resolveProductItem(itemReq);
            requireStockAvailable(resolved.sku(), itemReq.getQuantity(), resolved.productName());
            BigDecimal itemTotal = resolved.unitPrice().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            OrderItem item = OrderItem.builder()
                    .productId(itemReq.getProductId())
                    .productName(resolved.productName())
                    .productImage(itemReq.getProductImage())
                    .variantId(resolved.variantId())
                    .variantName(resolved.variantName())
                    .sku(resolved.sku())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(resolved.unitPrice())
                    .totalPrice(itemTotal)
                    .build();
            item.setOrder(order);
            orderItems.add(item);
            subtotal = subtotal.add(itemTotal);
        }

        order.setItems(orderItems);
        order.setSubtotal(subtotal);
        order.setTotalAmount(subtotal.add(DEFAULT_SHIPPING_FEE));
        requireExpectedTotalMatches(quote.getTotal(), order.getTotalAmount());

        order = orderRepository.save(order);
        try {
            reserveInventory(order);
        } catch (Exception ex) {
            compensateReservation(order);
            throw new BadRequestException("Không thể giữ tồn kho cho đơn hàng: " + ex.getMessage());
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
        if (oldStatus == OrderStatus.PENDING_PAYMENT
                && newStatus == OrderStatus.CONFIRMED
                && !Boolean.TRUE.equals(order.getIsPaid())) {
            throw new BadRequestException("VietQR payment must be confirmed before confirming the order");
        }

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
            order.getStatus() == OrderStatus.REFUNDED ||
            order.getStatus() == OrderStatus.EXPIRED) {
            throw new BadRequestException("Khong the huy don hang o trang thai hien tai");
        }
        if (order.getStatus() == OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("VietQR order cannot be cancelled while payment is active. Please wait for it to expire.");
        }

        OrderStatus oldStatus = order.getStatus();
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        // Hoàn lại stock nếu đã bị trừ, hoặc hủy reserve nếu chưa trừ
        if (Boolean.TRUE.equals(order.getStockDeducted())) {
            restoreInventory(order);
            order.setStockDeducted(false);
            orderRepository.save(order);
        } else {
            unreserveInventory(order);
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

    @Override
    @Transactional
    public void handlePaymentSuccess(String orderId, String paymentId) {
        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        requireMatchingPayment(order, paymentId);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.EXPIRED) {
            throw new BadRequestException("Cannot confirm payment for an inactive order");
        }
        if (Boolean.TRUE.equals(order.getIsPaid())
                && Boolean.TRUE.equals(order.getStockDeducted())
                && order.getStatus() == OrderStatus.CONFIRMED) {
            return;
        }
        OrderStatus oldStatus = order.getStatus();
        if (!Boolean.TRUE.equals(order.getStockDeducted())) {
            deductInventory(order);
            order.setStockDeducted(true);
        }
        order.setIsPaid(true);
        order.setPaidAt(LocalDateTime.now());
        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // TRỪ stock ngay khi xác nhận đơn hàng (đặt hàng thành công)
        clearCartAfterSuccess(order);
        if (oldStatus != OrderStatus.CONFIRMED) {
            createStatusHistory(orderId, oldStatus.name(), OrderStatus.CONFIRMED.name(), "Thanh toan thanh cong", null);
        }

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

        log.info("Payment success for order {}, inventory deducted and cart cleanup attempted", order.getOrderNumber());
    }

    @Override
    @Transactional
    public void handlePaymentFailure(String orderId, String paymentId, boolean expired) {
        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        requireMatchingPayment(order, paymentId);
        OrderStatus targetStatus = expired ? OrderStatus.EXPIRED : OrderStatus.CANCELLED;
        if (order.getStatus() == targetStatus
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.EXPIRED) {
            return;
        }
        if (Boolean.TRUE.equals(order.getIsPaid()) || Boolean.TRUE.equals(order.getStockDeducted())) {
            throw new BadRequestException("Cannot reject or expire an order after successful payment");
        }

        OrderStatus oldStatus = order.getStatus();
        unreserveInventory(order);
        order.setPaymentId(paymentId);
        order.setStatus(targetStatus);
        orderRepository.save(order);
        createStatusHistory(orderId, oldStatus.name(), targetStatus.name(),
                expired ? "Thanh toan VietQR het han" : "Thanh toan VietQR bi tu choi", null);
        log.info("Payment {} for order {}, reservation released", targetStatus, order.getOrderNumber());
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

        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED
                || order.getStatus() == OrderStatus.PENDING_PAYMENT
                || order.getStatus() == OrderStatus.EXPIRED) {
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
            case CONFIRMED -> from == OrderStatus.PENDING || from == OrderStatus.PENDING_PAYMENT || from == OrderStatus.PROCESSING;
            case PROCESSING -> from == OrderStatus.CONFIRMED;
            case SHIPPED -> from == OrderStatus.PROCESSING;
            case DELIVERED -> from == OrderStatus.SHIPPED;
            case CANCELLED -> from != OrderStatus.DELIVERED && from != OrderStatus.CANCELLED
                    && from != OrderStatus.REFUNDED && from != OrderStatus.EXPIRED;
            case REFUNDED -> from == OrderStatus.DELIVERED;
            case EXPIRED -> from == OrderStatus.PENDING_PAYMENT;
            case PENDING, PENDING_PAYMENT -> false;
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

        // CANCELLED: hoàn lại stock nếu đã trừ, hoặc hủy reserve nếu chưa trừ
        if (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.EXPIRED) {
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreInventory(order);
                order.setStockDeducted(false);
                orderRepository.save(order);
            } else {
                unreserveInventory(order);
            }
        }

        // REFUNDED: hoàn lại stock (khách đã nhận hàng nhưng được hoàn tiền), hoặc hủy reserve nếu chưa trừ
        if (newStatus == OrderStatus.REFUNDED) {
            if (Boolean.TRUE.equals(order.getStockDeducted())) {
                restoreInventory(order);
                order.setStockDeducted(false);
                orderRepository.save(order);
            } else {
                unreserveInventory(order);
            }
        }
    }

    private void reserveInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            InventoryActionRequest req = inventoryRequest(item, order, "Reserve for order ");
            requireInventorySuccess(inventoryClient.reserveStock(req), "reserve", item.getSku());
        }
    }

    private void unreserveInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            InventoryActionRequest req = inventoryRequest(item, order, "Unreserve for cancelled/expired order ");
            requireInventorySuccess(inventoryClient.unreserveStock(req), "unreserve", item.getSku());
        }
    }

    private void deductInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            InventoryActionRequest req = inventoryRequest(item, order, "Deduct for order ");
            requireInventorySuccess(inventoryClient.deductStock(req), "deduct", item.getSku());
        }
    }

    private void restoreInventory(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            InventoryActionRequest req = inventoryRequest(item, order, "Restore stock for cancelled/refunded order ");
            requireInventorySuccess(inventoryClient.restoreStock(req), "restore", item.getSku());
        }
    }

    private InventoryActionRequest inventoryRequest(OrderItem item, Order order, String notePrefix) {
        InventoryActionRequest req = new InventoryActionRequest();
        req.setSku(item.getSku());
        req.setQuantity(item.getQuantity());
        req.setOrderId(order.getId());
        req.setNote(notePrefix + order.getOrderNumber());
        return req;
    }

    private void requireInventorySuccess(ApiResponse<Void> response, String action, String sku) {
        if (response == null || !response.isSuccess()) {
            String detail = response != null ? response.getMessage() : "no response";
            throw new BadRequestException("Inventory " + action + " failed for SKU " + sku + ": " + detail);
        }
    }

    private void compensateReservation(Order order) {
        for (OrderItem item : order.getItems()) {
            if (item.getSku() == null) continue;
            try {
                InventoryActionRequest req = inventoryRequest(item, order, "Compensate reservation for order ");
                requireInventorySuccess(inventoryClient.unreserveStock(req), "compensating unreserve", item.getSku());
            } catch (Exception ex) {
                log.error("Failed to compensate inventory reservation for SKU {}: {}", item.getSku(), ex.getMessage());
            }
        }
    }

    private boolean clearCartAfterSuccess(Order order) {
        String userId = order.getUserId();
        if (!StringUtils.hasText(userId)) {
            log.info("Skipping cart clear after payment success for orderId={} because userId is empty", order.getId());
            return false;
        }
        try {
            ApiResponse<Void> response = cartClient.clearCart(userId);
            if (response == null || !response.isSuccess()) {
                String detail = response != null ? response.getMessage() : "no response";
                log.warn("Cart clear failed after payment success for orderId={}, userId={}: {}",
                        order.getId(), userId, detail);
                return false;
            }
            return true;
        } catch (Exception ex) {
            log.warn("Cart clear failed after payment success for orderId={}, userId={}: {}",
                    order.getId(), userId, ex.getMessage());
            return false;
        }
    }

    private void requireMatchingPayment(Order order, String paymentId) {
        if (order.getPaymentId() != null && !Objects.equals(order.getPaymentId(), paymentId)) {
            throw new BadRequestException("Payment does not belong to this order");
        }
    }

    private CheckoutQuoteDTO buildCheckoutQuote(List<OrderItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new BadRequestException("Checkout items are required");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<CheckoutQuoteItemDTO> items = new ArrayList<>();
        for (OrderItemRequest request : requests) {
            ResolvedProductItem resolved = resolveProductItem(request);
            requireStockAvailable(resolved.sku(), request.getQuantity(), resolved.productName());
            BigDecimal lineTotal = resolved.unitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
            items.add(CheckoutQuoteItemDTO.builder()
                    .productId(request.getProductId())
                    .productName(resolved.productName())
                    .productImage(request.getProductImage())
                    .variantId(resolved.variantId())
                    .variantName(resolved.variantName())
                    .sku(resolved.sku())
                    .quantity(request.getQuantity())
                    .unitPrice(resolved.unitPrice())
                    .lineTotal(lineTotal)
                    .build());
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        return CheckoutQuoteDTO.builder()
                .items(items)
                .subtotal(subtotal)
                .shippingFee(DEFAULT_SHIPPING_FEE)
                .discount(discount)
                .total(subtotal.add(DEFAULT_SHIPPING_FEE).subtract(discount))
                .build();
    }

    private void requireExpectedTotalMatches(BigDecimal expectedTotal, BigDecimal authoritativeTotal) {
        if (expectedTotal == null || expectedTotal.compareTo(authoritativeTotal) != 0) {
            throw new BadRequestException("Checkout total changed. Please refresh the quote before placing the order.");
        }
    }

    private void requireVoucherSupport(String voucherCode) {
        if (voucherCode != null && !voucherCode.isBlank()) {
            throw new BadRequestException("Promotion codes are not supported yet");
        }
    }

    private ResolvedProductItem resolveProductItem(OrderItemRequest request) {
        ApiResponse<ProductClient.ProductDTO> response;
        try {
            response = productClient.getProductById(request.getProductId());
        } catch (Exception ex) {
            throw new BadRequestException("Cannot load product catalog data for " + request.getProductId());
        }
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BadRequestException("Product catalog data is unavailable for " + request.getProductId());
        }

        ProductClient.ProductDTO product = response.getData();
        ProductClient.ProductVariantDTO variant = null;
        if (product.getVariants() != null && !product.getVariants().isEmpty()) {
            if (request.getVariantId() != null && !request.getVariantId().isBlank()) {
                variant = product.getVariants().stream()
                        .filter(candidate -> Objects.equals(candidate.getId(), request.getVariantId()))
                        .findFirst()
                        .orElse(null);
            } else if (request.getSku() != null && !request.getSku().isBlank()) {
                variant = product.getVariants().stream()
                        .filter(candidate -> Objects.equals(candidate.getSku(), request.getSku()))
                        .findFirst()
                        .orElse(null);
            } else {
                variant = product.getVariants().get(0);
            }
            if (variant == null) {
                throw new BadRequestException("Selected product variant does not exist");
            }
        }

        String sku = variant != null ? variant.getSku() : request.getSku();
        BigDecimal unitPrice = variant != null && variant.getPrice() != null
                ? variant.getPrice()
                : product.getBasePrice();
        if (sku == null || sku.isBlank()) {
            throw new BadRequestException("Product SKU is required");
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new BadRequestException("Product price is invalid");
        }
        if (request.getQuantity() <= 0) {
            throw new BadRequestException("Product quantity must be positive");
        }
        return new ResolvedProductItem(
                product.getName(),
                variant != null ? variant.getId() : request.getVariantId(),
                variant != null ? variant.getVariantName() : request.getVariantName(),
                sku,
                unitPrice);
    }

    private void requireStockAvailable(String sku, int quantity, String productName) {
        ApiResponse<StockCheckResponse> response;
        try {
            response = inventoryClient.checkStock(sku, quantity);
        } catch (Exception ex) {
            throw new BadRequestException("Cannot verify stock for SKU " + sku);
        }
        if (response == null || !response.isSuccess() || response.getData() == null) {
            throw new BadRequestException("Stock availability is unavailable for SKU " + sku);
        }
        if (!Boolean.TRUE.equals(response.getData().getInStock())) {
            throw new BadRequestException("Product is out of stock: " + productName + " (SKU: " + sku + ")");
        }
    }

    private OrderDTO enrichPaymentDetails(OrderDTO order, CreatePaymentResponse payment) {
        if (payment == null) {
            return order;
        }
        order.setPaymentStatus(payment.getPaymentStatus());
        order.setCurrency(payment.getCurrency());
        order.setBankCode(payment.getBankCode());
        order.setBankName(payment.getBankName());
        order.setAccountNumber(payment.getAccountNumber());
        order.setAccountName(payment.getAccountName());
        order.setTransferContent(payment.getTransferContent());
        order.setQrUrl(payment.getQrUrl());
        order.setExpiredAt(payment.getExpiredAt());
        return order;
    }

    private record ResolvedProductItem(
            String productName,
            String variantId,
            String variantName,
            String sku,
            BigDecimal unitPrice) {
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
                .orderStatus(order.getStatus())
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
        clearCartAfterSuccess(order);

        createStatusHistory(orderId, order.getStatus().name(), order.getStatus().name(), "Cập nhật thanh toán thủ công", changedBy);

        log.info("Order {} payment status marked as paid by {}", order.getOrderNumber(), changedBy);
        return toDTO(order);
    }
}
