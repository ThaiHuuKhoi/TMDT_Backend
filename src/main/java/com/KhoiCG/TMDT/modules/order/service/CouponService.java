package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.order.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.order.entity.Coupon;
import com.KhoiCG.TMDT.modules.order.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        return couponRepository.save(coupon);
    }

    public CouponResponse applyCoupon(CouponCheckRequest request) {
        Coupon coupon = couponRepository.findByCode(request.getCode().toUpperCase())
                .orElse(null);

        if (coupon == null || !Boolean.TRUE.equals(coupon.getIsActive())) {
            return new CouponResponse(false, "Mã không hợp lệ", 0L, request.getOrderAmount());
        }
        if (coupon.getExpiryDate() != null && LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            return new CouponResponse(false, "Mã đã hết hạn", 0L, request.getOrderAmount());
        }
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            return new CouponResponse(false, "Mã đã hết lượt dùng", 0L, request.getOrderAmount());
        }

        BigDecimal orderAmountBD = BigDecimal.valueOf(request.getOrderAmount());

        if (coupon.getMinOrderValue() != null && orderAmountBD.compareTo(coupon.getMinOrderValue()) < 0) {
            return new CouponResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu", 0L, request.getOrderAmount());
        }

        BigDecimal discountBD = BigDecimal.ZERO;

        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discountBD = orderAmountBD.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discountBD = coupon.getDiscountValue();
        }

        if (discountBD.compareTo(orderAmountBD) > 0) {
            discountBD = orderAmountBD;
        }

        long discount = discountBD.longValue();
        long finalPrice = request.getOrderAmount() - discount;

        return new CouponResponse(true, "Áp dụng thành công", discount, finalPrice);
    }
}