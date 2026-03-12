package com.KhoiCG.TMDT.modules.shipping.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ShippingLogResponse {
    private String status;
    private String message;
    private String reportedAt;
    private String createdAt;
}

