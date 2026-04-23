package com.melodyshop.order.service;

import com.melodyshop.common.exception.BadRequestException;
import com.melodyshop.common.exception.ResourceNotFoundException;
import com.melodyshop.order.dto.*;
import com.melodyshop.order.entity.Coupon;
import com.melodyshop.order.entity.enums.CouponType;
import com.melodyshop.order.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public CouponDTO validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new BadRequestException("Mã giảm giá không tồn tại: " + code));
        if (!coupon.getIsActive()) throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
        if (coupon.getExpiresAt() != null && coupon.getExpiresAt().isBefore(LocalDateTime.now()))
            throw new BadRequestException("Mã giảm giá đã hết hạn");
        if (coupon.getMaxUses() > 0 && coupon.getUsedCount() >= coupon.getMaxUses())
            throw new BadRequestException("Mã giảm giá đã hết lượt sử dụng");
        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0)
            throw new BadRequestException("Đơn hàng tối thiểu " + coupon.getMinOrderAmount() + " để áp dụng mã này");

        BigDecimal discountAmount;
        if (coupon.getType() == CouponType.PERCENT) {
            discountAmount = orderAmount.multiply(coupon.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.FLOOR);
            if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0)
                discountAmount = coupon.getMaxDiscount();
        } else {
            discountAmount = coupon.getValue();
        }
        CouponDTO dto = toDTO(coupon);
        dto.setDiscountAmount(discountAmount);
        return dto;
    }

    @Transactional
    public void incrementUsedCount(String code) {
        couponRepository.findByCode(code.toUpperCase()).ifPresent(c -> {
            c.setUsedCount(c.getUsedCount() + 1);
            couponRepository.save(c);
        });
    }

    public Page<CouponDTO> getAllCoupons(Pageable pageable) {
        return couponRepository.findAll(pageable).map(this::toDTO);
    }

    @Transactional
    public CouponDTO createCoupon(CreateCouponRequest req) {
        if (couponRepository.existsByCode(req.getCode().toUpperCase()))
            throw new BadRequestException("Mã coupon đã tồn tại: " + req.getCode());
        CouponType type;
        try { type = CouponType.valueOf(req.getType().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Loại coupon không hợp lệ"); }

        Coupon coupon = Coupon.builder()
                .code(req.getCode().toUpperCase()).type(type).value(req.getValue())
                .minOrderAmount(req.getMinOrderAmount() != null ? req.getMinOrderAmount() : BigDecimal.ZERO)
                .maxDiscount(req.getMaxDiscount())
                .maxUses(req.getMaxUses() != null ? req.getMaxUses() : 0)
                .expiresAt(req.getExpiresAt()).isActive(true).build();
        return toDTO(couponRepository.save(coupon));
    }

    @Transactional
    public CouponDTO updateCoupon(String id, CreateCouponRequest req) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
        if (req.getValue() != null) coupon.setValue(req.getValue());
        if (req.getMinOrderAmount() != null) coupon.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getMaxDiscount() != null) coupon.setMaxDiscount(req.getMaxDiscount());
        if (req.getMaxUses() != null) coupon.setMaxUses(req.getMaxUses());
        if (req.getExpiresAt() != null) coupon.setExpiresAt(req.getExpiresAt());
        return toDTO(couponRepository.save(coupon));
    }

    private CouponDTO toDTO(Coupon c) {
        return CouponDTO.builder().id(c.getId()).code(c.getCode()).type(c.getType().name())
                .value(c.getValue()).minOrderAmount(c.getMinOrderAmount()).maxDiscount(c.getMaxDiscount())
                .maxUses(c.getMaxUses()).usedCount(c.getUsedCount()).expiresAt(c.getExpiresAt())
                .isActive(c.getIsActive()).build();
    }
}
