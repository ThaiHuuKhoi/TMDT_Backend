package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CartSummaryResponse {
    private Long id;
    private int totalItems;
    private BigDecimal totalAmount;
    private List<CartItemSummaryResponse> items;
}
