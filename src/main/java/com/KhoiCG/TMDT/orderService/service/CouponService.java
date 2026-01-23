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

    // 1. Tạo Coupon mới (Dành cho Admin)
    public Coupon createCoupon(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    // 2. Kiểm tra và tính toán giảm giá (Dành cho User)
    public CouponResponse applyCoupon(CouponCheckRequest request) {
        // Tìm coupon trong DB
        Coupon coupon = couponRepository.findByCode(request.getCode())
                .orElse(null);

        // -- VALIDATION LAYER --

        // A. Mã không tồn tại hoặc bị tắt
        if (coupon == null || !coupon.isActive()) {
            return new CouponResponse(false, "Mã giảm giá không tồn tại hoặc đã bị khóa.", 0.0, request.getOrderAmount());
        }

        // B. Mã hết hạn
        if (coupon.getExpiryDate() != null && LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            return new CouponResponse(false, "Mã giảm giá đã hết hạn.", 0.0, request.getOrderAmount());
        }

        // C. Hết lượt dùng
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            return new CouponResponse(false, "Mã giảm giá đã hết lượt sử dụng.", 0.0, request.getOrderAmount());
        }

        // D. Đơn hàng chưa đủ điều kiện tối thiểu
        if (coupon.getMinOrderValue() != null && request.getOrderAmount() < coupon.getMinOrderValue()) {
            return new CouponResponse(false, "Đơn hàng phải từ " + coupon.getMinOrderValue() + "đ mới được áp dụng.", 0.0, request.getOrderAmount());
        }

        // -- CALCULATION LAYER --
        double discount = 0.0;

        if ("PERCENT".equals(coupon.getDiscountType())) {
            // Giảm theo % (Ví dụ: 10%)
            discount = request.getOrderAmount() * (coupon.getDiscountValue() / 100);
        } else if ("FIXED".equals(coupon.getDiscountType())) {
            // Giảm thẳng tiền (Ví dụ: 50k)
            discount = coupon.getDiscountValue();
        }

        // Đảm bảo số tiền giảm không vượt quá giá trị đơn hàng (Không âm tiền)
        if (discount > request.getOrderAmount()) {
            discount = request.getOrderAmount();
        }

        double finalPrice = request.getOrderAmount() - discount;

        return new CouponResponse(true, "Áp dụng mã thành công!", discount, finalPrice);
    }
}