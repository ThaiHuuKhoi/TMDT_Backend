package com.KhoiCG.TMDT.modules.coupon.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.coupon.dto.BestCouponOffer;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import com.KhoiCG.TMDT.modules.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @CacheEvict(value = "coupons", allEntries = true)
    public Coupon createCoupon(Coupon coupon) {
        coupon.setCode(coupon.getCode().toUpperCase());
        if (coupon.getUsedCount() == null) coupon.setUsedCount(0);
        if (coupon.getLimitPerUser() == null) coupon.setLimitPerUser(1);
        if (coupon.getIsActive() == null) coupon.setIsActive(true);
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
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            return new CouponResponse(false, "Mã đã hết lượt dùng", 0L, request.getOrderAmount());
        }

        BigDecimal orderAmountBD = BigDecimal.valueOf(request.getOrderAmount());

        if (coupon.getMinOrderValue() != null && orderAmountBD.compareTo(coupon.getMinOrderValue()) < 0) {
            return new CouponResponse(false, "Đơn hàng chưa đạt giá trị tối thiểu", 0L, request.getOrderAmount());
        }

        long discount = computeDiscountAmount(coupon, request.getOrderAmount());
        long finalPrice = request.getOrderAmount() - discount;

        return new CouponResponse(true, "Áp dụng thành công", discount, finalPrice);
    }

    public Optional<BestCouponOffer> findBestOfferForOrderAmount(long orderAmount) {
        if (orderAmount <= 0) {
            return Optional.empty();
        }
        long bestDiscount = 0L;
        Coupon best = null;
        for (Coupon c : couponRepository.findByIsActiveTrue()) {
            if (!canApplyCoupon(c, orderAmount)) {
                continue;
            }
            long d = computeDiscountAmount(c, orderAmount);
            if (d > bestDiscount) {
                bestDiscount = d;
                best = c;
            }
        }
        if (best == null || bestDiscount <= 0) {
            return Optional.empty();
        }
        long finalPrice = orderAmount - bestDiscount;
        return Optional.of(new BestCouponOffer(best.getCode(), bestDiscount, finalPrice));
    }

    public List<Coupon> getAllCoupons() {
        return couponRepository.findAll();
    }

    @CacheEvict(value = "coupons", allEntries = true)
    public void toggleCouponStatus(Long id, boolean isActive) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", "Không tìm thấy mã giảm giá!"));
        coupon.setIsActive(isActive);
        couponRepository.save(coupon);
    }

    private boolean canApplyCoupon(Coupon coupon, long orderAmount) {
        if (coupon == null || !Boolean.TRUE.equals(coupon.getIsActive())) {
            return false;
        }
        if (coupon.getExpiryDate() != null && LocalDateTime.now().isAfter(coupon.getExpiryDate())) {
            return false;
        }
        if (coupon.getMaxUsage() != null && coupon.getUsedCount() != null && coupon.getUsedCount() >= coupon.getMaxUsage()) {
            return false;
        }
        BigDecimal orderAmountBD = BigDecimal.valueOf(orderAmount);
        if (coupon.getMinOrderValue() != null && orderAmountBD.compareTo(coupon.getMinOrderValue()) < 0) {
            return false;
        }
        return true;
    }

    private long computeDiscountAmount(Coupon coupon, long orderAmount) {
        BigDecimal orderAmountBD = BigDecimal.valueOf(orderAmount);
        BigDecimal discountBD;
        if (coupon.getDiscountType() == Coupon.DiscountType.PERCENTAGE) {
            discountBD = orderAmountBD.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        } else {
            discountBD = coupon.getDiscountValue();
        }
        if (discountBD.compareTo(orderAmountBD) > 0) {
            discountBD = orderAmountBD;
        }
        return discountBD.longValue();
    }
}
