package com.melodyshop.order.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.entity.*;
import com.melodyshop.order.entity.enums.OrderStatus;
import com.melodyshop.order.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusLogRepository statusLogRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CouponService couponService;

    @Value("${order.shipping-fee:30000}")
    private BigDecimal defaultShippingFee;

    /**
     * Tạo đơn hàng từ giỏ hàng.
     * Flow: validate cart → apply coupon → create order + items → log status → clear cart
     */
    @Transactional
    public OrderDTO createOrder(String userId, CreateOrderRequest request) {
        // 1. Validate cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Giỏ hàng trống"));

        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Giỏ hàng trống, không thể tạo đơn hàng");
        }

        // 2. Tính subtotal
        BigDecimal subtotal = cart.getItems().stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Apply coupon nếu có
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponDTO couponResult = couponService.validateCoupon(request.getCouponCode(), subtotal);
            discountAmount = couponResult.getDiscountAmount();
            couponService.incrementUsedCount(request.getCouponCode());
        }

        // 4. Tính total
        BigDecimal totalAmount = subtotal.subtract(discountAmount).add(defaultShippingFee);
        if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            totalAmount = BigDecimal.ZERO;
        }

        // 5. Generate order code: ORD-yyyyMMdd-xxx
        String orderCode = generateOrderCode();

        // 6. Tạo Order
        Order order = Order.builder()
                .orderCode(orderCode)
                .userId(userId)
                .status(OrderStatus.PENDING)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .shippingFee(defaultShippingFee)
                .totalAmount(totalAmount)
                .couponCode(request.getCouponCode())
                .receiverName(request.getReceiverName())
                .receiverPhone(request.getReceiverPhone())
                .shippingProvince(request.getShippingProvince())
                .shippingDistrict(request.getShippingDistrict())
                .shippingWard(request.getShippingWard())
                .shippingAddress(request.getShippingAddress())
                .note(request.getNote())
                .build();
        order = orderRepository.save(order);

        // 7. Tạo OrderItems (snapshot từ cart items)
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productId(cartItem.getProductId())
                    .variantId(cartItem.getVariantId())
                    .sku(cartItem.getSku())
                    .productName(cartItem.getProductName())
                    .variantName(cartItem.getVariantName())
                    .unitPrice(cartItem.getUnitPrice())
                    .quantity(cartItem.getQuantity())
                    .subtotal(cartItem.getUnitPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())))
                    .imageUrl(cartItem.getImageUrl())
                    .build();
            order.getItems().add(orderItem);
        }
        order = orderRepository.save(order);

        // 8. Ghi status log
        createStatusLog(order.getId(), null, OrderStatus.PENDING.name(), "Đơn hàng được tạo", userId);

        // 9. Xóa giỏ hàng
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order created: {} for user {}", orderCode, userId);
        return toDTO(order);
    }

    /**
     * Danh sách đơn hàng của customer.
     */
    public Page<OrderDTO> getMyOrders(String userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(this::toDTO);
    }

    /**
     * Chi tiết đơn hàng.
     */
    public OrderDTO getOrderById(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem đơn hàng này");
        }

        return toDTO(order);
    }

    /**
     * Timeline trạng thái đơn hàng.
     */
    public List<OrderStatusLogDTO> getOrderTimeline(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền xem đơn hàng này");
        }

        return statusLogRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toLogDTO)
                .collect(Collectors.toList());
    }

    /**
     * Hủy đơn hàng (chỉ khi PENDING hoặc CONFIRMED).
     */
    @Transactional
    public OrderDTO cancelOrder(String userId, String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        if (!order.getUserId().equals(userId)) {
            throw new BadRequestException("Bạn không có quyền hủy đơn hàng này");
        }

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BadRequestException("Chỉ có thể hủy đơn ở trạng thái PENDING hoặc CONFIRMED");
        }

        String oldStatus = order.getStatus().name();
        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        createStatusLog(orderId, oldStatus, OrderStatus.CANCELLED.name(), "Khách hàng hủy đơn", userId);

        log.info("Order {} cancelled by user {}", order.getOrderCode(), userId);
        return toDTO(order);
    }

    // ==================== Admin APIs ====================

    /**
     * Danh sách đơn hàng (Admin, filter theo status).
     */
    public Page<OrderDTO> getAdminOrders(String status, Pageable pageable) {
        if (status != null && !status.isBlank()) {
            try {
                OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
                return orderRepository.findByStatusOrderByCreatedAtDesc(orderStatus, pageable).map(this::toDTO);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái không hợp lệ: " + status);
            }
        }
        return orderRepository.findAllOrders(pageable).map(this::toDTO);
    }

    /**
     * Cập nhật trạng thái đơn hàng (Admin).
     */
    @Transactional
    public OrderDTO updateOrderStatus(String adminId, String orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng", "id", orderId));

        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(request.getNewStatus().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái không hợp lệ: " + request.getNewStatus());
        }

        validateStatusTransition(order.getStatus(), newStatus);

        String oldStatus = order.getStatus().name();
        order.setStatus(newStatus);
        orderRepository.save(order);

        createStatusLog(orderId, oldStatus, newStatus.name(), request.getNote(), adminId);

        log.info("Order {} status updated: {} → {} by admin {}", order.getOrderCode(), oldStatus, newStatus, adminId);
        return toDTO(order);
    }

    // ==================== Private helpers ====================

    private String generateOrderCode() {
        String datePrefix = "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = orderRepository.countByOrderCodePrefix(datePrefix);
        return datePrefix + "-" + String.format("%03d", count + 1);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        boolean valid = switch (current) {
            case PENDING -> target == OrderStatus.CONFIRMED || target == OrderStatus.CANCELLED;
            case CONFIRMED -> target == OrderStatus.PREPARING || target == OrderStatus.CANCELLED;
            case PREPARING -> target == OrderStatus.SHIPPING;
            case SHIPPING -> target == OrderStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Không thể chuyển trạng thái từ %s sang %s", current, target));
        }
    }

    private void createStatusLog(String orderId, String oldStatus, String newStatus, String note, String changedBy) {
        OrderStatusLog logEntry = OrderStatusLog.builder()
                .orderId(orderId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .note(note)
                .changedBy(changedBy)
                .build();
        statusLogRepository.save(logEntry);
    }

    private OrderDTO toDTO(Order order) {
        List<OrderItemDTO> items = order.getItems().stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());

        return OrderDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .userId(order.getUserId())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .discountAmount(order.getDiscountAmount())
                .shippingFee(order.getShippingFee())
                .totalAmount(order.getTotalAmount())
                .couponCode(order.getCouponCode())
                .receiverName(order.getReceiverName())
                .receiverPhone(order.getReceiverPhone())
                .shippingProvince(order.getShippingProvince())
                .shippingDistrict(order.getShippingDistrict())
                .shippingWard(order.getShippingWard())
                .shippingAddress(order.getShippingAddress())
                .note(order.getNote())
                .items(items)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderItemDTO toItemDTO(OrderItem item) {
        return OrderItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .variantId(item.getVariantId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .imageUrl(item.getImageUrl())
                .build();
    }

    private OrderStatusLogDTO toLogDTO(OrderStatusLog logEntry) {
        return OrderStatusLogDTO.builder()
                .id(logEntry.getId())
                .oldStatus(logEntry.getOldStatus())
                .newStatus(logEntry.getNewStatus())
                .note(logEntry.getNote())
                .changedBy(logEntry.getChangedBy())
                .createdAt(logEntry.getCreatedAt())
                .build();
    }
}
