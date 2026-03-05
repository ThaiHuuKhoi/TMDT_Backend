package com.KhoiCG.TMDT.modules.marketing.dto;

import com.KhoiCG.TMDT.modules.marketing.entity.TargetType;
import lombok.Data;

@Data
public class BannerRequest {
    private String title;
    private String description;
    private String imageUrl;
    private TargetType targetType;
    private Long targetId;
    private String linkUrl;
    private Integer displayOrder;
}