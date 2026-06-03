package com.KhoiCG.TMDT.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductStatResponse {
    private Long productId;
    private String productName;
    private Long unitsSold;
    private BigDecimal revenue;
}
