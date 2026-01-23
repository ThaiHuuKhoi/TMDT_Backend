package com.KhoiCG.TMDT.orderService.controller;

import com.KhoiCG.TMDT.orderService.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.orderService.dto.CouponResponse; // Sửa lại kiểu trả về rõ ràng
import com.KhoiCG.TMDT.orderService.entity.Coupon;
import com.KhoiCG.TMDT.orderService.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    // 🔒 CHỈ ADMIN ĐƯỢC TẠO MÃ
    @PostMapping("/create")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Coupon> createCoupon(@RequestBody Coupon coupon) {
        return ResponseEntity.ok(couponService.createCoupon(coupon));
    }

    // 🔓 User check mã (Ai cũng được check)
    @PostMapping("/apply")
    public ResponseEntity<CouponResponse> applyCoupon(@RequestBody CouponCheckRequest request) {
        return ResponseEntity.ok(couponService.applyCoupon(request));
    }
}