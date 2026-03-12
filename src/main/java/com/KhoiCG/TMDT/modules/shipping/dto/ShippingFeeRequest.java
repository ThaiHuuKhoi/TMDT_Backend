package com.KhoiCG.TMDT.modules.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class ShippingFeeRequest {
    @NotBlank
    private String providerCode; // GHN, GHTK

    @NotNull
    private Integer toDistrictId;

    @NotBlank
    private String toWardCode;

    @Positive
    private int weightInGrams;
}

