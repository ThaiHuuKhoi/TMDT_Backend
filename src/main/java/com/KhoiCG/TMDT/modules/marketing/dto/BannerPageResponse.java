package com.KhoiCG.TMDT.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BannerPageResponse {
    private List<BannerResponse> items;
    private int page;
    private int size;
    private long totalItems;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
