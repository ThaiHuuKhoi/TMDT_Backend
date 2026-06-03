package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.product.dto.ProductPageResponse;
import com.KhoiCG.TMDT.modules.product.dto.ProductResponse;
import com.KhoiCG.TMDT.modules.product.service.ProductService;
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
import org.springframework.http.MediaType;
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
        controllers = ProductController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, ProductControllerWebMvcTest.TestSecurityConfig.class})
class ProductControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

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
    @DisplayName("GET /api/products trả về items và pageInfo")
    void getProducts_ReturnsWrappedPageResponse() throws Exception {
        ProductResponse product = ProductResponse.builder()
                .id(1L)
                .name("iPhone 15")
                .slug("iphone-15")
                .build();
        ProductPageResponse response = ProductPageResponse.builder()
                .items(List.of(product))
                .page(0)
                .size(20)
                .totalItems(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(productService.getProducts(any(), any(), any(), anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/products").param("page", "0").param("size", "20").param("search", "iphone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].name").value("iPhone 15"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/products bị chặn với non-admin")
    void createProduct_ForbiddenForNonAdmin() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "name", "iPhone",
                "description", "desc",
                "categorySlug", "dien-thoai",
                "variants", List.of(Map.of(
                        "sku", "IP-001",
                        "price", 10,
                        "stockQuantity", 1,
                        "attributes", Map.of("Mau", "Do")
                ))
        ));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isForbidden());

        verify(productService, never()).createProduct(any());
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("POST /api/products trả VALIDATION_ERROR khi payload không hợp lệ")
    void createProduct_ReturnsValidationError() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "name", "",
                "description", "",
                "categorySlug", "",
                "variants", List.of()
        ));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(productService, never()).createProduct(any());
    }
}
