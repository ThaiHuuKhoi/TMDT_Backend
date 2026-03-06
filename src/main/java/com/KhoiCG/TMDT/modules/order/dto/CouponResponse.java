package com.KhoiCG.TMDT.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CouponResponse {
    private boolean valid;
    private String message;
    private Long discountAmount;
    private Long finalPrice;
}