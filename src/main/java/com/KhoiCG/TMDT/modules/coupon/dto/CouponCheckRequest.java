package com.KhoiCG.TMDT.modules.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CouponCheckRequest {
    @NotBlank(message = "code là bắt buộc")
    private String code;

    @NotNull(message = "orderAmount là bắt buộc")
    @Min(value = 1, message = "orderAmount phải lớn hơn hoặc bằng 1")
    private Long orderAmount;
}
