package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.order.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.order.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.order.entity.Coupon;
import com.KhoiCG.TMDT.modules.order.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }

    @PostMapping("/apply")
    public ResponseEntity<CouponResponse> applyCoupon(@RequestBody CouponCheckRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<java.util.List<Coupon>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAllCoupons());
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> toggleStatus(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> payload) {
        couponService.toggleCouponStatus(id, payload.get("isActive"));
        return ResponseEntity.ok("Cập nhật trạng thái thành công!");
    }
}