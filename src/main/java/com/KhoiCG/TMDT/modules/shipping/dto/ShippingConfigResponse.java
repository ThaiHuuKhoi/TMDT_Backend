package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingConfigResponse {
    private Long defaultFeeVnd;
    private Integer gramsPerItemUnit;
}
