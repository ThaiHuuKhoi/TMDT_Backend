package com.KhoiCG.TMDT.modules.marketing.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FacebookConnectionStatusResponse {
    private boolean configured;
    private String pageId;
    private String message;
}
