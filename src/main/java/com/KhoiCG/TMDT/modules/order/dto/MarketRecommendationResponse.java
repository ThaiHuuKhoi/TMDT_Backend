package com.KhoiCG.TMDT.modules.order.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MarketRecommendationResponse {
    private String generatedAt;
    private int lookbackDays;
    private int horizonDays;
    private int totalCandidates;
    private String formula;
    private List<MarketRecommendationItemResponse> items;
}
