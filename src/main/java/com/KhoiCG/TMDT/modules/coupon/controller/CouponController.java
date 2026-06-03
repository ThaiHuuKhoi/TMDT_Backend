package com.KhoiCG.TMDT.modules.coupon.controller;

import com.KhoiCG.TMDT.modules.coupon.dto.CouponAdminResponse;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.coupon.dto.CouponStatusUpdateRequest;
import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import com.KhoiCG.TMDT.modules.coupon.service.CouponService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<CouponAdminResponse> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(toCouponAdminResponse(couponService.createCoupon(coupon)));
    }

    @PostMapping("/apply")
    public ResponseEntity<CouponResponse> applyCoupon(@Valid @RequestBody CouponCheckRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<CouponAdminResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons().stream().map(this::toCouponAdminResponse).toList());
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> toggleStatus(
            @PathVariable Long id,
            @Valid @RequestBody CouponStatusUpdateRequest request) {
        couponService.toggleCouponStatus(id, request.getIsActive());
        return ResponseEntity.ok("Cập nhật trạng thái thành công!");
    }

    private CouponAdminResponse toCouponAdminResponse(Coupon coupon) {
        return CouponAdminResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxUsage(coupon.getMaxUsage())
                .usedCount(coupon.getUsedCount())
                .limitPerUser(coupon.getLimitPerUser())
                .expiryDate(coupon.getExpiryDate())
                .isActive(coupon.getIsActive())
                .build();
    }
}
