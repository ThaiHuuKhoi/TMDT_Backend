package com.KhoiCG.TMDT.modules.payment.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateSessionRequest {
    @Size(max = 50, message = "couponCode tối đa 50 ký tự")
    private String couponCode;

    @NotNull(message = "Thiếu thông tin giao hàng")
    @Valid
    private ShippingDetailsRequest shipping;
}