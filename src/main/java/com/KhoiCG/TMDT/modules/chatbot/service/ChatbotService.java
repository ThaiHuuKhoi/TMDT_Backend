package com.KhoiCG.TMDT.modules.chatbot.service;

import com.KhoiCG.TMDT.modules.chatbot.dto.ChatRequest;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatResponse;
import com.KhoiCG.TMDT.modules.chatbot.entity.ChatMessage;
import com.KhoiCG.TMDT.modules.chatbot.entity.ChatSession;
import com.KhoiCG.TMDT.modules.chatbot.provider.ChatAdvisor;
import com.KhoiCG.TMDT.modules.chatbot.repository.ChatMessageRepository;
import com.KhoiCG.TMDT.modules.chatbot.repository.ChatSessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChatbotService {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final Map<String, ChatAdvisor> advisors;

    @Autowired
    public ChatbotService(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            List<ChatAdvisor> advisorList
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.advisors = advisorList.stream()
                .collect(Collectors.toMap(a -> a.getCode().toUpperCase(), Function.identity()));
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        ChatSession session = resolveSession(request.getSessionId());

        List<ChatMessage> recent = chatMessageRepository.findTop20BySessionIdOrderByCreatedAtDesc(session.getId());
        recent = recent.stream()
                .sorted(Comparator.comparing(ChatMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        ChatMessage userMsg = ChatMessage.builder()
                .role(ChatMessage.Role.USER)
                .content(request.getMessage())
                .provider("CLIENT")
                .build();
        session.addMessage(userMsg);

        ChatAdvisor advisor = pickAdvisor();
        String answer = advisor.reply(request.getMessage(), recent);

        ChatMessage botMsg = ChatMessage.builder()
                .role(ChatMessage.Role.ASSISTANT)
                .content(answer)
                .provider(advisor.getCode())
                .build();
        session.addMessage(botMsg);

        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .sessionId(session.getId())
                .answer(answer)
                .provider(advisor.getCode())
                .build();
    }

    private ChatAdvisor pickAdvisor() {
        ChatAdvisor openAi = advisors.get("OPENAI");
        if (openAi != null) {
            return openAi;
        }
        ChatAdvisor rules = advisors.get("RULES");
        if (rules == null) {
            throw new RuntimeException("Chatbot chưa có advisor phù hợp (missing RULES)");
        }
        return rules;
    }

    private ChatSession resolveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return chatSessionRepository.save(ChatSession.newAnonymous());
        }
        return chatSessionRepository.findById(sessionId)
                .orElseGet(() -> chatSessionRepository.save(ChatSession.builder().id(sessionId).build()));
    }
}

