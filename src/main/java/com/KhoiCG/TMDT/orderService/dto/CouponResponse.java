package com.KhoiCG.TMDT.orderService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponResponse {
    private boolean valid;
    private String message;
    private Long discountAmount; // Số tiền được giảm
    private Long finalPrice;     // Giá sau khi giảm
}