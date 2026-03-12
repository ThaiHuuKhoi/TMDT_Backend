package com.KhoiCG.TMDT.modules.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;

    @NotBlank
    private String message;
}

