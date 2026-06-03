package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MarketRecommendationItemResponse {
    private Long productId;
    private String productName;
    private String categorySlug;
    private String categoryName;

    private long unitsSold30d;
    private int availableStock;
    private double daysOfCover;

    private int marketDemandIndex;
    private int internalSalesMomentum;
    private int stockRiskIndex;
    private int opportunityScore;

    private int recommendedQty;
    private String priority;
    private String reason;
}
