package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Data;

@Data
public class CouponCheckRequest {
    private String code;
    private Long orderAmount;
}