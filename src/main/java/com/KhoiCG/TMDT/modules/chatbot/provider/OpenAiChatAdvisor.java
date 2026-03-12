package com.KhoiCG.TMDT.modules.chatbot.provider;

import com.KhoiCG.TMDT.modules.chatbot.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiChatAdvisor implements ChatAdvisor {

    private final RestTemplate restTemplate;

    @Value("${chatbot.openai.apiKey:}")
    private String apiKey;

    @Value("${chatbot.openai.baseUrl:https://api.openai.com}")
    private String baseUrl;

    @Value("${chatbot.openai.model:gpt-4o-mini}")
    private String model;

    @Override
    public String getCode() {
        return "OPENAI";
    }

    @Override
    public String reply(String userMessage, List<ChatMessage> recentHistory) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("Thiếu cấu hình chatbot.openai.apiKey");
        }

        String url = baseUrl + "/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.3);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of(
                "role", "system",
                "content", "Bạn là chatbot tư vấn TMĐT. Trả lời ngắn gọn, đúng trọng tâm, tiếng Việt. Nếu thiếu dữ liệu (mã đơn/mã vận đơn), hãy hỏi lại."
        ));

        if (recentHistory != null) {
            for (ChatMessage m : recentHistory) {
                if (m == null || m.getContent() == null) continue;
                String role = switch (m.getRole()) {
                    case USER -> "user";
                    case ASSISTANT -> "assistant";
                    default -> "system";
                };
                messages.add(Map.of("role", role, "content", m.getContent()));
            }
        }

        messages.add(Map.of("role", "user", "content", userMessage));
        body.put("messages", messages);

        try {
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, entity, Map.class);
            Map<String, Object> respBody = resp.getBody();

            if (respBody == null) {
                throw new RuntimeException("OpenAI response body is null");
            }

            Object choicesObj = respBody.get("choices");
            if (!(choicesObj instanceof List<?> choices) || choices.isEmpty()) {
                throw new RuntimeException("OpenAI response missing choices");
            }

            Object first = choices.get(0);
            if (!(first instanceof Map<?, ?> firstMap)) {
                throw new RuntimeException("OpenAI response invalid choice format");
            }

            Object messageObj = firstMap.get("message");
            if (!(messageObj instanceof Map<?, ?> messageMap)) {
                throw new RuntimeException("OpenAI response missing message");
            }

            Object contentObj = messageMap.get("content");
            String content = contentObj == null ? null : String.valueOf(contentObj);
            if (content == null || content.isBlank()) {
                throw new RuntimeException("OpenAI response empty content");
            }

            return content.trim();
        } catch (Exception e) {
            log.error("OpenAI advisor error", e);
            throw new RuntimeException("Không thể gọi AI tư vấn lúc này. Bạn thử lại sau giúp mình nhé.", e);
        }
    }
}

