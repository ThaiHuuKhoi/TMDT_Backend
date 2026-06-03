package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.order.dto.OrderPageResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.http.HttpStatus;

@WebMvcTest(
        controllers = OrderController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, OrderControllerWebMvcTest.TestSecurityConfig.class})
class OrderControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private com.KhoiCG.TMDT.modules.order.mapper.OrderMapper orderMapper;

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
    @DisplayName("GET /api/orders trả về items + pageInfo")
    void getAllOrders_ReturnsWrappedPageResponse() throws Exception {
        OrderResponse order = OrderResponse.builder().id(1L).status("PENDING").build();
        OrderPageResponse response = OrderPageResponse.builder()
                .items(List.of(order))
                .page(0)
                .size(10)
                .totalItems(1)
                .totalPages(1)
                .hasNext(false)
                .hasPrevious(false)
                .build();
        when(orderService.getAllOrdersPageForAdmin(anyInt(), anyInt())).thenReturn(response);

        mockMvc.perform(get("/api/orders").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("PATCH /api/orders/admin/{id}/status bị chặn với non-admin")
    void updateOrderStatus_ForbiddenForNonAdmin() throws Exception {
        mockMvc.perform(patch("/api/orders/admin/2/status")
                        .contentType("application/json")
                        .content("{\"status\":\"COMPLETED\"}"))
                .andExpect(status().isForbidden());

        verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("PATCH /api/orders/admin/{id}/status trả VALIDATION_ERROR khi status rỗng")
    void updateOrderStatus_ReturnsValidationErrorWhenStatusBlank() throws Exception {
        mockMvc.perform(patch("/api/orders/admin/2/status")
                        .contentType("application/json")
                        .content("{\"status\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.status").exists());

        verify(orderService, never()).updateOrderStatus(anyLong(), anyString());
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("PATCH /api/orders/admin/{id}/status trả INVALID_ORDER_TRANSITION khi chuyển trạng thái sai")
    void updateOrderStatus_ReturnsInvalidTransitionError() throws Exception {
        when(orderService.updateOrderStatus(anyLong(), anyString()))
                .thenThrow(new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "INVALID_ORDER_TRANSITION",
                        "Không thể chuyển trạng thái từ COMPLETED sang PENDING"
                ));

        mockMvc.perform(patch("/api/orders/admin/2/status")
                        .contentType("application/json")
                        .content("{\"status\":\"PENDING\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER_TRANSITION"));
    }
}
