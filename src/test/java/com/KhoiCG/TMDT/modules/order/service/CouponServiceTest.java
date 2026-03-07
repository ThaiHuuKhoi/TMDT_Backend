package com.KhoiCG.TMDT.modules.order.service;

import com.KhoiCG.TMDT.modules.order.dto.CouponCheckRequest;
import com.KhoiCG.TMDT.modules.order.dto.CouponResponse;
import com.KhoiCG.TMDT.modules.order.entity.Coupon;
import com.KhoiCG.TMDT.modules.order.repository.CouponRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private CouponService couponService;

    // =========================================================
    // TEST HÀM CREATE COUPON
    // =========================================================

    @Test
    @DisplayName("Tạo Coupon: Phải tự động in hoa mã code trước khi lưu")
    void createCoupon_ShouldUpperCaseCode() {
        Coupon coupon = new Coupon();
        coupon.setCode("giamgia50"); // Chữ thường

        // Trả về chính cái coupon được truyền vào hàm save
        when(couponRepository.save(any(Coupon.class))).thenAnswer(i -> i.getArgument(0));

        Coupon savedCoupon = couponService.createCoupon(coupon);

        assertEquals("GIAMGIA50", savedCoupon.getCode());
        verify(couponRepository, times(1)).save(any(Coupon.class));
    }

    // =========================================================
    // TEST HÀM APPLY COUPON - CÁC TRƯỜNG HỢP LỖI (INVALID)
    // =========================================================

    @Test
    @DisplayName("Áp dụng: Thất bại khi mã không tồn tại")
    void applyCoupon_NotFound() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("SAIMA");
        req.setOrderAmount(500000L);

        when(couponRepository.findByCode("SAIMA")).thenReturn(Optional.empty());

        CouponResponse response = couponService.applyCoupon(req);

        assertFalse(response.isValid());
        assertEquals("Mã không hợp lệ", response.getMessage());
        assertEquals(0L, response.getDiscountAmount());
        assertEquals(500000L, response.getFinalPrice());
    }

    @Test
    @DisplayName("Áp dụng: Thất bại khi mã bị khóa (isActive = false)")
    void applyCoupon_Inactive() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("KHOA");
        req.setOrderAmount(500000L);

        Coupon coupon = Coupon.builder().code("KHOA").isActive(false).build();
        when(couponRepository.findByCode("KHOA")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertFalse(response.isValid());
        assertEquals("Mã không hợp lệ", response.getMessage());
    }

    @Test
    @DisplayName("Áp dụng: Thất bại khi mã đã quá hạn sử dụng")
    void applyCoupon_Expired() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("HETHAN");
        req.setOrderAmount(500000L);

        Coupon coupon = Coupon.builder()
                .code("HETHAN")
                .isActive(true)
                .expiryDate(LocalDateTime.now().minusDays(1)) // Quá hạn 1 ngày
                .build();
        when(couponRepository.findByCode("HETHAN")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertFalse(response.isValid());
        assertEquals("Mã đã hết hạn", response.getMessage());
    }

    @Test
    @DisplayName("Áp dụng: Thất bại khi đã dùng hết lượt (usedCount >= maxUsage)")
    void applyCoupon_MaxUsageReached() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("HETLUOT");
        req.setOrderAmount(500000L);

        Coupon coupon = Coupon.builder()
                .code("HETLUOT")
                .isActive(true)
                .maxUsage(100)
                .usedCount(100) // Đã dùng 100/100
                .build();
        when(couponRepository.findByCode("HETLUOT")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertFalse(response.isValid());
        assertEquals("Mã đã hết lượt dùng", response.getMessage());
    }

    @Test
    @DisplayName("Áp dụng: Thất bại khi đơn hàng chưa đủ giá trị tối thiểu")
    void applyCoupon_MinOrderValueNotMet() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("MIN200K");
        req.setOrderAmount(150000L); // Mua 150k

        Coupon coupon = Coupon.builder()
                .code("MIN200K")
                .isActive(true)
                .minOrderValue(BigDecimal.valueOf(200000)) // Đòi 200k
                .build();
        when(couponRepository.findByCode("MIN200K")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertFalse(response.isValid());
        assertEquals("Đơn hàng chưa đạt giá trị tối thiểu", response.getMessage());
    }

    // =========================================================
    // TEST HÀM APPLY COUPON - CÁC TRƯỜNG HỢP THÀNH CÔNG (VALID)
    // =========================================================

    @Test
    @DisplayName("Áp dụng: Thành công tính tiền theo PHẦN TRĂM (PERCENTAGE)")
    void applyCoupon_SuccessPercentage() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("GIAM10");
        req.setOrderAmount(200000L); // Đơn hàng 200,000

        Coupon coupon = Coupon.builder()
                .code("GIAM10")
                .isActive(true)
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.valueOf(10)) // Giảm 10%
                .build();

        when(couponRepository.findByCode("GIAM10")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertTrue(response.isValid());
        assertEquals("Áp dụng thành công", response.getMessage());
        assertEquals(20000L, response.getDiscountAmount()); // 10% của 200k là 20k
        assertEquals(180000L, response.getFinalPrice()); // Còn 180k
    }

    @Test
    @DisplayName("Áp dụng: Thành công tính tiền TRỪ THẲNG (FIXED)")
    void applyCoupon_SuccessFixed() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("TRU50K");
        req.setOrderAmount(200000L); // Đơn hàng 200,000

        Coupon coupon = Coupon.builder()
                .code("TRU50K")
                .isActive(true)
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(50000)) // Trừ 50,000
                .build();

        when(couponRepository.findByCode("TRU50K")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertTrue(response.isValid());
        assertEquals("Áp dụng thành công", response.getMessage());
        assertEquals(50000L, response.getDiscountAmount());
        assertEquals(150000L, response.getFinalPrice());
    }

    @Test
    @DisplayName("Áp dụng: Đảm bảo số tiền giảm KHÔNG vượt quá giá trị đơn hàng")
    void applyCoupon_CapDiscountAtOrderAmount() {
        CouponCheckRequest req = new CouponCheckRequest();
        req.setCode("TRU50K");
        req.setOrderAmount(30000L); // Đơn hàng chỉ có 30,000

        Coupon coupon = Coupon.builder()
                .code("TRU50K")
                .isActive(true)
                .discountType(Coupon.DiscountType.FIXED)
                .discountValue(BigDecimal.valueOf(50000)) // Giảm tận 50,000
                .build();

        when(couponRepository.findByCode("TRU50K")).thenReturn(Optional.of(coupon));

        CouponResponse response = couponService.applyCoupon(req);

        assertTrue(response.isValid());
        assertEquals("Áp dụng thành công", response.getMessage());
        // Tiền giảm phải bị chặn lại ở mức 30k (bằng đúng tiền hàng)
        assertEquals(30000L, response.getDiscountAmount());
        // Tổng tiền không thể âm, tối thiểu là 0đ
        assertEquals(0L, response.getFinalPrice());
    }
}