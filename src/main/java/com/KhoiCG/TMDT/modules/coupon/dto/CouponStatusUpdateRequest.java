package com.KhoiCG.TMDT.modules.coupon.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponStatusUpdateRequest {
    @NotNull(message = "isActive là bắt buộc")
    private Boolean isActive;
}
