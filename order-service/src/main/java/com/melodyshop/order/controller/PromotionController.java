package com.melodyshop.order.controller;

import com.melodyshop.common.dto.ApiResponse;
import com.melodyshop.order.dto.ApplyPromoCodeRequest;
import com.melodyshop.order.dto.ApplyPromoCodeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/promotions")
@RequiredArgsConstructor
public class PromotionController {

    // In-memory promotion codes for demo (in production, use database)
    private static final Map<String, PromotionCode> PROMOTION_CODES = new HashMap<>();

    static {
        // Welcome discount - 10% off
        PROMOTION_CODES.put("WELCOME10", PromotionCode.builder()
                .code("WELCOME10")
                .type(PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("10"))
                .minOrderAmount(new BigDecimal("0"))
                .maxDiscount(new BigDecimal("100000"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusYears(1))
                .active(true)
                .build());

        // Summer sale - 15% off
        PROMOTION_CODES.put("SUMMER15", PromotionCode.builder()
                .code("SUMMER15")
                .type(PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("15"))
                .minOrderAmount(new BigDecimal("200000"))
                .maxDiscount(new BigDecimal("200000"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusMonths(3))
                .active(true)
                .build());

        // Fixed amount - 50k off
        PROMOTION_CODES.put("SAVE50K", PromotionCode.builder()
                .code("SAVE50K")
                .type(PromotionType.FIXED_AMOUNT)
                .discountValue(new BigDecimal("50000"))
                .minOrderAmount(new BigDecimal("300000"))
                .maxDiscount(null)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusMonths(2))
                .active(true)
                .build());

        // VIP discount - 20% off
        PROMOTION_CODES.put("VIP20", PromotionCode.builder()
                .code("VIP20")
                .type(PromotionType.PERCENTAGE)
                .discountValue(new BigDecimal("20"))
                .minOrderAmount(new BigDecimal("500000"))
                .maxDiscount(new BigDecimal("500000"))
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusYears(1))
                .active(true)
                .build());

        // Free shipping
        PROMOTION_CODES.put("FREESHIP", PromotionCode.builder()
                .code("FREESHIP")
                .type(PromotionType.FREE_SHIPPING)
                .discountValue(new BigDecimal("50000"))
                .minOrderAmount(new BigDecimal("100000"))
                .maxDiscount(null)
                .validFrom(LocalDateTime.now().minusDays(1))
                .validTo(LocalDateTime.now().plusMonths(6))
                .active(true)
                .build());
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<ApplyPromoCodeResponse>> applyPromoCode(
            @RequestBody ApplyPromoCodeRequest request,
            @RequestParam(required = false) BigDecimal orderAmount) {
        
        String code = request.getCode().toUpperCase().trim();
        log.info("Applying promo code: {} for order amount: {}", code, orderAmount);

        PromotionCode promo = PROMOTION_CODES.get(code);

        if (promo == null) {
            return ResponseEntity.ok(ApiResponse.<ApplyPromoCodeResponse>builder()
                    .success(false)
                    .message("Mã khuyến mãi không tồn tại")
                    .data(null)
                    .build());
        }

        // Check if promo is active
        if (!promo.isActive()) {
            return ResponseEntity.ok(ApiResponse.<ApplyPromoCodeResponse>builder()
                    .success(false)
                    .message("Mã khuyến mãi đã bị vô hiệu hóa")
                    .data(null)
                    .build());
        }

        // Check validity period
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(promo.getValidFrom()) || now.isAfter(promo.getValidTo())) {
            return ResponseEntity.ok(ApiResponse.<ApplyPromoCodeResponse>builder()
                    .success(false)
                    .message("Mã khuyến mãi đã hết hạn hoặc chưa có hiệu lực")
                    .data(null)
                    .build());
        }

        // Check minimum order amount
        if (orderAmount != null && promo.getMinOrderAmount() != null 
                && orderAmount.compareTo(promo.getMinOrderAmount()) < 0) {
            return ResponseEntity.ok(ApiResponse.<ApplyPromoCodeResponse>builder()
                    .success(false)
                    .message("Đơn hàng tối thiểu " + formatCurrency(promo.getMinOrderAmount()) + " để sử dụng mã này")
                    .data(null)
                    .build());
        }

        ApplyPromoCodeResponse response = ApplyPromoCodeResponse.builder()
                .valid(true)
                .code(promo.getCode())
                .type(promo.getType().name())
                .discountValue(promo.getDiscountValue())
                .minOrderAmount(promo.getMinOrderAmount())
                .maxDiscount(promo.getMaxDiscount())
                .message("Áp dụng mã giảm giá thành công")
                .build();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/codes")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAvailableCodes() {
        Map<String, String> codes = new HashMap<>();
        codes.put("WELCOME10", "Giảm 10% cho đơn hàng mới");
        codes.put("SUMMER15", "Giảm 15% cho đơn từ 200K");
        codes.put("SAVE50K", "Giảm 50K cho đơn từ 300K");
        codes.put("VIP20", "VIP - Giảm 20% cho đơn từ 500K");
        codes.put("FREESHIP", "Miễn phí vận chuyển cho đơn từ 100K");
        return ResponseEntity.ok(ApiResponse.ok(codes));
    }

    private String formatCurrency(BigDecimal amount) {
        return String.format("%,.0fđ", amount);
    }

    public static BigDecimal calculateDiscount(BigDecimal orderAmount, ApplyPromoCodeResponse promo) {
        if (promo == null || !promo.isValid()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount;
        if ("PERCENTAGE".equals(promo.getType())) {
            discount = orderAmount.multiply(promo.getDiscountValue())
                    .divide(new BigDecimal("100"), 0, RoundingMode.HALF_UP);
            // Apply max discount cap
            if (promo.getMaxDiscount() != null && discount.compareTo(promo.getMaxDiscount()) > 0) {
                discount = promo.getMaxDiscount();
            }
        } else if ("FIXED_AMOUNT".equals(promo.getType())) {
            discount = promo.getDiscountValue();
        } else if ("FREE_SHIPPING".equals(promo.getType())) {
            discount = promo.getDiscountValue();
        } else {
            discount = BigDecimal.ZERO;
        }

        // Discount cannot exceed order amount
        if (discount.compareTo(orderAmount) > 0) {
            discount = orderAmount;
        }

        return discount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    private static class PromotionCode {
        private String code;
        private PromotionType type;
        private BigDecimal discountValue;
        private BigDecimal minOrderAmount;
        private BigDecimal maxDiscount;
        private LocalDateTime validFrom;
        private LocalDateTime validTo;
        private boolean active;
    }

    private enum PromotionType {
        PERCENTAGE,
        FIXED_AMOUNT,
        FREE_SHIPPING
    }
}
