package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class CartItemSummaryResponse {
    private Long variantId;
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
}
