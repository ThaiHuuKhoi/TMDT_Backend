package com.KhoiCG.TMDT.orderService.service;

import com.KhoiCG.TMDT.orderService.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.orderService.dto.CouponResponse;
import com.KhoiCG.TMDT.orderService.entity.Coupon;
import com.KhoiCG.TMDT.orderService.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public Coupon createCoupon(Coupon coupon) {
        // Nên uppercase mã code để user nhập thường/hoa đều được
        coupon.setCode(coupon.getCode().toUpperCase());
        return couponRepository.save(coupon);
    }

    public CouponResponse applyCoupon(CouponCheckRequest request) {
        // Tìm mã (đã uppercase)
        Coupon coupon = couponRepository.findByCode(request.getCode().toUpperCase())
                .orElse(null);

        // 1. Validate
        if (coupon == null || !coupon.isActive()) {
            return new CouponResponse(false, "Mã không hợp lệ", 0L, request.getOrderAmount());
        }
        if (coupon.getExpiryDate() != null && LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            return new CouponResponse(false, "Mã đã hết hạn", 0L, request.getOrderAmount());
        }
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            return new CouponResponse(false, "Mã đã hết lượt dùng", 0L, request.getOrderAmount());
        }
        if (coupon.getMinOrderValue() != null && request.getOrderAmount() < coupon.getMinOrderValue()) {
            return new CouponResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu", 0L, request.getOrderAmount());
        }

        // 2. Tính toán (Dùng Long toàn bộ)
        long discount = 0;
        long orderAmount = request.getOrderAmount().longValue(); // Convert Double -> Long nếu DTO vẫn là Double

        if ("PERCENT".equals(coupon.getDiscountType())) {
            // Công thức: (Tổng * % ) / 100
            discount = (orderAmount * coupon.getDiscountValue()) / 100;
        } else {
            discount = coupon.getDiscountValue();
        }

        // Không giảm quá giá trị đơn hàng
        if (discount > orderAmount) discount = orderAmount;

        long finalPrice = orderAmount - discount;

        return new CouponResponse(true, "Áp dụng thành công", discount, finalPrice);
    }
}