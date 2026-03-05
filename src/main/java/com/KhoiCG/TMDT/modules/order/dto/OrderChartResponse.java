// File: src/main/java/com/KhoiCG/TMDT/modules/order/dto/OrderChartResponse.java
package com.KhoiCG.TMDT.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderChartResponse {
    private String month;
    private Long total;
    private Long successful;
}