package com.KhoiCG.TMDT.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BannerResponse {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String targetUrl;
    private int displayOrder;
}