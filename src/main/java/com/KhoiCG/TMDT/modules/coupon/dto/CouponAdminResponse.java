package com.KhoiCG.TMDT.modules.coupon.dto;

import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CouponAdminResponse {
    private Long id;
    private String code;
    private Coupon.DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private Integer maxUsage;
    private Integer usedCount;
    private Integer limitPerUser;
    private LocalDateTime expiryDate;
    private Boolean isActive;
}
