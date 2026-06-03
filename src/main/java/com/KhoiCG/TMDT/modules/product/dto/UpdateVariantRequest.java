package com.KhoiCG.TMDT.modules.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateVariantRequest {

    @NotNull(message = "Giá không được để trống")
    @Min(value = 0, message = "Giá không được âm")
    private BigDecimal price;

    private BigDecimal originalPrice;

    @NotNull(message = "Số lượng tồn kho không được để trống")
    @Min(value = 0, message = "Tồn kho không được âm")
    private Integer stockQuantity;

    private Boolean isActive;
}
