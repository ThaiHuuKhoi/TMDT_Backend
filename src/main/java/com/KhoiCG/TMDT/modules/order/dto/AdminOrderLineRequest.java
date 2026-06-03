package com.KhoiCG.TMDT.modules.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AdminOrderLineRequest {
    @NotNull(message = "variantId là bắt buộc")
    private Long variantId;

    @NotNull(message = "quantity là bắt buộc")
    @Min(value = 1, message = "quantity phải >= 1")
    private Integer quantity;
}
