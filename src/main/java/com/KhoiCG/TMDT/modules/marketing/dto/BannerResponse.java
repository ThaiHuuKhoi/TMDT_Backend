package com.KhoiCG.TMDT.modules.marketing.dto;

import com.KhoiCG.TMDT.modules.marketing.entity.TargetType;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class BannerResponse implements Serializable {
    private Long id;
    private String title;
    private String description;
    private String imageUrl;
    private String targetUrl;
    private Integer displayOrder;
    private TargetType targetType;
    private Long targetId;
    private String linkUrl;
    private Boolean isActive;
}