package com.KhoiCG.TMDT.modules.marketing.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerPageResponse;
import com.KhoiCG.TMDT.modules.marketing.dto.BannerResponse;
import com.KhoiCG.TMDT.modules.marketing.service.BannerService;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = BannerController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, BannerControllerWebMvcTest.TestSecurityConfig.class})
class BannerControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BannerService bannerService;

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
    @DisplayName("GET /api/banners/admin/all trả về items + pageInfo")
    void getAllBannersForAdmin_ReturnsWrappedPageResponse() throws Exception {
        BannerResponse banner = BannerResponse.builder().id(1L).title("Sale").build();
        BannerPageResponse response = BannerPageResponse.builder()
                .items(List.of(banner))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(bannerService.getAllBannersPage(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/banners/admin/all").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Sale"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/banners bị chặn với non-admin")
    void createBanner_ForbiddenForNonAdmin() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", "Flash Sale",
                "imageUrl", "https://cdn/banner.jpg",
                "targetType", "EXTERNAL_LINK",
                "linkUrl", "https://example.com"
        ));

        mockMvc.perform(post("/api/banners")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isForbidden());

        verify(bannerService, never()).saveBanner(any());
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("POST /api/banners trả VALIDATION_ERROR khi payload không hợp lệ")
    void createBanner_ReturnsValidationError() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "title", "",
                "imageUrl", "",
                "targetType", "EXTERNAL_LINK"
        ));

        mockMvc.perform(post("/api/banners")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.imageUrl").exists());

        verify(bannerService, never()).saveBanner(any());
    }
}
