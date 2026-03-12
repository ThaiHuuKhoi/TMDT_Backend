package com.KhoiCG.TMDT.modules.chatbot.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatResponse {
    private String sessionId;
    private String answer;
    private String provider;
}

