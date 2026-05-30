package com.melodyshop.order.dto;

import com.melodyshop.order.enums.OrderStatus;
import com.melodyshop.order.enums.PaymentMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private String id;
    private String userId;
    private String orderNumber;
    private OrderStatus status;
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingCity;
    private String shippingEmail;
    private String shippingPostalCode;
    private String orderNote;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private String paymentId;
    private String paymentUrl;
    private Boolean isPaid;
    private LocalDateTime paidAt;
    private Boolean stockDeducted;
    private List<OrderItemDTO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
