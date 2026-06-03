package com.KhoiCG.TMDT.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyRevenuePoint {
    private String month;
    private BigDecimal revenue;
}
