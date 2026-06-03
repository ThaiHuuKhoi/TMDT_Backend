package com.KhoiCG.TMDT.modules.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BestCouponOffer {
    private String code;
    private long discountAmount;
    private long finalPrice;
}
