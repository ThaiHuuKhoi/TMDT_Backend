package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingFeeResponse {
    private String providerCode;
    private double fee;
    private String currency;
}

