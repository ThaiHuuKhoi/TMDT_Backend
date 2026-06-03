package com.KhoiCG.TMDT.modules.chatbot.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatResponse;
import com.KhoiCG.TMDT.modules.chatbot.service.ChatbotRateLimiter;
import com.KhoiCG.TMDT.modules.chatbot.service.ChatbotService;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ChatbotController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "application.security.trust-forward-headers=false",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, ChatbotControllerWebMvcTest.TestSecurityConfig.class})
class ChatbotControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatbotService chatbotService;

    @MockitoBean
    private ChatbotRateLimiter chatbotRateLimiter;

    @MockitoBean
    private UserRepo userRepo;

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("default");
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/chatbot/chat trả VALIDATION_ERROR khi message rỗng")
    void chat_ReturnsValidationErrorWhenMessageBlank() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("message", ""));

        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.message").exists());

        verify(chatbotRateLimiter, never()).assertAllowed(anyString(), any());
        verify(chatbotService, never()).chat(any(), any());
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/chatbot/chat trả CHATBOT_RATE_LIMITED khi quá hạn mức")
    void chat_ReturnsRateLimitError() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("message", "xin chao"));
        when(userRepo.findByEmail("user@tmdt.com")).thenReturn(Optional.empty());
        org.mockito.Mockito.doThrow(new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CHATBOT_RATE_LIMITED", "Quá nhiều"))
                .when(chatbotRateLimiter).assertAllowed(anyString(), any());

        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("CHATBOT_RATE_LIMITED"));

        verify(chatbotService, never()).chat(any(), any());
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/chatbot/chat thành công trả sessionId/answer/provider")
    void chat_ReturnsResponseOnSuccess() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("message", "xin chao"));
        when(userRepo.findByEmail("user@tmdt.com")).thenReturn(Optional.empty());
        when(chatbotService.chat(any(), any())).thenReturn(ChatResponse.builder()
                .sessionId("s1")
                .answer("hello")
                .provider("RULES")
                .build());

        mockMvc.perform(post("/api/chatbot/chat")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionId").value("s1"))
                .andExpect(jsonPath("$.answer").value("hello"))
                .andExpect(jsonPath("$.provider").value("RULES"));
    }
}
