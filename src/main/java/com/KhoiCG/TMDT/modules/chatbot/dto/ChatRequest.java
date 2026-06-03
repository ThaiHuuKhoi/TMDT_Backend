package com.KhoiCG.TMDT.modules.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChatRequest {
    private String sessionId;

    @NotBlank
    @Size(max = 4000)
    private String message;
}

