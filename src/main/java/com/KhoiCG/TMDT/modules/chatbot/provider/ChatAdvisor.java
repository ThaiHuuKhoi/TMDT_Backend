package com.KhoiCG.TMDT.modules.chatbot.provider;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatMessage;

import java.util.List;

public interface ChatAdvisor {
    String getCode();

    String reply(String userMessage, List<ChatMessage> recentHistory);
}

