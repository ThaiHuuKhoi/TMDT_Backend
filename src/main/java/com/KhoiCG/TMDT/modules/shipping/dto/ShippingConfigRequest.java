package com.KhoiCG.TMDT.modules.shipping.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ShippingConfigRequest {
    @NotNull
    @Min(0)
    private Long defaultFeeVnd;

    @NotNull
    @Min(1)
    private Integer gramsPerItemUnit;
}
