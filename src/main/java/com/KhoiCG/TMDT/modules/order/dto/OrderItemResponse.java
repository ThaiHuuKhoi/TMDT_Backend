package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderItemResponse {
    private String productName;
    private String sku;
    private String variantInfo;
    private Integer quantity;
    private String priceFormatted;
}