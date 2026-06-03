package com.KhoiCG.TMDT.modules.marketing.dto;

import com.KhoiCG.TMDT.modules.marketing.entity.SocialPlatform;
import com.KhoiCG.TMDT.modules.marketing.entity.SocialPostStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SocialPostResponse {
    private Long id;
    private SocialPlatform platform;
    private String message;
    private String linkUrl;
    private String imageUrl;
    private SocialPostStatus status;
    private String externalPostId;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
}
