package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingOrderResponse {
    private String trackingCode;
    private double shippingFee;
    private String expectedDeliveryTime;
}