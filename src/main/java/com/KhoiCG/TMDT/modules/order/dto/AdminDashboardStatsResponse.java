package com.KhoiCG.TMDT.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardStatsResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private Map<String, Long> ordersByStatus;
    private List<OrderChartResponse> monthlyOrders;
    private List<MonthlyRevenuePoint> monthlyRevenue;
    private List<TopProductStatResponse> topProducts;
}
