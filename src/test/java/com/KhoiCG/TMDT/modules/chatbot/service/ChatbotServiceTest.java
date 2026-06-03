package com.KhoiCG.TMDT.modules.chatbot.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatRequest;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatResponse;
import com.KhoiCG.TMDT.modules.chatbot.entity.ChatSession;
import com.KhoiCG.TMDT.modules.chatbot.provider.ChatAdvisor;
import com.KhoiCG.TMDT.modules.chatbot.repository.ChatMessageRepository;
import com.KhoiCG.TMDT.modules.chatbot.repository.ChatSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {
    @Mock
    private ChatSessionRepository chatSessionRepository;
    @Mock
    private ChatMessageRepository chatMessageRepository;
    @Mock
    private ChatAdvisor ruleAdvisor;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        lenient().when(ruleAdvisor.getCode()).thenReturn("RULES");
        lenient().when(ruleAdvisor.reply(any(), any())).thenReturn("hello");
        chatbotService = new ChatbotService(chatSessionRepository, chatMessageRepository, List.of(ruleAdvisor));
    }

    @Test
    @DisplayName("Ẩn danh không được tạo session theo sessionId client tự đưa")
    void chat_Anonymous_DoesNotReuseClientProvidedUnknownSessionId() {
        ChatRequest request = new ChatRequest();
        request.setSessionId("client-supplied-id");
        request.setMessage("Xin chao");

        when(chatSessionRepository.findById("client-supplied-id")).thenReturn(Optional.empty());
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(i -> i.getArgument(0));
        when(chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        ChatResponse response = chatbotService.chat(request, null);

        assertNotNull(response.getSessionId());
        assertNotEquals("client-supplied-id", response.getSessionId());
        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        org.mockito.Mockito.verify(chatSessionRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        assertTrue(captor.getAllValues().stream().noneMatch(s -> "client-supplied-id".equals(s.getId())));
    }

    @Test
    @DisplayName("Ném ApiException chuẩn khi thiếu RULES advisor")
    void chat_ThrowsApiExceptionWhenRulesAdvisorMissing() {
        ChatbotService serviceWithoutRules = new ChatbotService(chatSessionRepository, chatMessageRepository, List.of());
        ChatRequest request = new ChatRequest();
        request.setMessage("hello");
        when(chatSessionRepository.save(any(ChatSession.class))).thenAnswer(i -> i.getArgument(0));
        when(chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(any())).thenReturn(List.of());

        ApiException ex = assertThrows(ApiException.class, () -> serviceWithoutRules.chat(request, null));
        assertEquals("CHATBOT_PROVIDER_UNAVAILABLE", ex.getCode());
    }
}
