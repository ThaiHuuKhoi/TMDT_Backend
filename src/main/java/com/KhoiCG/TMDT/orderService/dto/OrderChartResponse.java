package com.KhoiCG.TMDT.orderService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OrderChartResponse {
    private String month;
    private long total;
    private long successful;
}