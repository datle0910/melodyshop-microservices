package com.melodyshop.order.repository;

import com.melodyshop.order.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, String> {
    Optional<Coupon> findByCode(String code);
    boolean existsByCode(String code);
}
