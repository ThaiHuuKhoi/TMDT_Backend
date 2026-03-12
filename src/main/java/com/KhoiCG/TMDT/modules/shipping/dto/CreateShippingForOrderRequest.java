package com.KhoiCG.TMDT.modules.shipping.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class CreateShippingForOrderRequest {
    @NotBlank
    private String providerCode; // GHN, GHTK

    @NotBlank
    private String customerName;

    @NotBlank
    private String customerPhone;

    @NotBlank
    private String address;

    @NotNull
    private Integer toDistrictId;

    @NotBlank
    private String toWardCode;

    @Positive
    private int weightInGrams;

    // Usually 0 for prepaid orders; keep flexible.
    private long codAmount;
}

