package com.KhoiCG.TMDT.orderService.dto;

import lombok.Data;

@Data
public class CouponCheckRequest {
    private String code;
    private Double orderAmount;
}