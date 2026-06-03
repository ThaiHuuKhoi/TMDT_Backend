package com.KhoiCG.TMDT.modules.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotBlank(message = "status là bắt buộc")
    private String status;
}
