package com.KhoiCG.TMDT.modules.user.controller;

import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.modules.user.dto.UserPageResponse;
import com.KhoiCG.TMDT.modules.user.dto.UserResponse;
import com.KhoiCG.TMDT.modules.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, UserControllerWebMvcTest.TestSecurityConfig.class})
class UserControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

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
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("GET /api/users trả về items và pageInfo")
    void getAllUsers_ReturnsWrappedPageResponse() throws Exception {
        UserResponse user = UserResponse.builder()
                .id(1L)
                .email("user@tmdt.com")
                .name("User A")
                .role("USER")
                .isActive(true)
                .build();
        UserPageResponse response = UserPageResponse.builder()
                .items(List.of(user))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(userService.getAllUsers(anyInt(), anyInt(), anyString(), any())).thenReturn(response);

        mockMvc.perform(get("/api/users").param("page", "0").param("size", "20").param("q", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("user@tmdt.com"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("PATCH /api/users/{id}/status bị chặn với non-admin")
    void updateUserStatus_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(patch("/api/users/2/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isActive\":false}"))
                .andExpect(status().isForbidden());

        verify(userService, never()).updateUserStatus(any(), any(), anyString());
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("PUT /api/users/profile trả VALIDATION_ERROR khi dữ liệu không hợp lệ")
    void updateProfile_ReturnsValidationError() throws Exception {
        String invalidName = "a".repeat(101);
        String payload = objectMapper.writeValueAsString(new java.util.HashMap<String, String>() {{
            put("name", invalidName);
        }});

        mockMvc.perform(put("/api/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.name").exists());

        verify(userService, never()).updateProfile(anyString(), any());
    }
}
