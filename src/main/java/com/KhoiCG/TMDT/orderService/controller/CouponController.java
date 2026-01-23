package com.KhoiCG.TMDT.orderService.controller;

import com.KhoiCG.TMDT.orderService.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.orderService.entity.Coupon;
import com.KhoiCG.TMDT.orderService.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // API cho Admin tạo mã
    @PostMapping("/create")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }

    // API cho User check mã
    @PostMapping("/apply")
    public ResponseEntity<?> applyCoupon(@RequestBody CouponCheckRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }
}