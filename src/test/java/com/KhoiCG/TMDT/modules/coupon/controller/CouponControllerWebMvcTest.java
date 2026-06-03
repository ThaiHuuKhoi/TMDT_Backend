package com.KhoiCG.TMDT.modules.coupon.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import com.KhoiCG.TMDT.modules.coupon.service.CouponService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = CouponController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, CouponControllerWebMvcTest.TestSecurityConfig.class})
class CouponControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CouponService couponService;

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
    @DisplayName("POST /api/coupons/apply trả VALIDATION_ERROR khi payload không hợp lệ")
    void applyCoupon_ReturnsValidationErrorWhenPayloadInvalid() throws Exception {
        mockMvc.perform(post("/api/coupons/apply")
                        .contentType("application/json")
                        .content("{\"code\":\"\",\"orderAmount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.code").exists())
                .andExpect(jsonPath("$.errors.orderAmount").exists());

        verify(couponService, never()).applyCoupon(any());
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("GET /api/coupons/admin/all trả về coupon DTO thay vì entity")
    void getAllCoupons_ReturnsCouponAdminResponse() throws Exception {
        Coupon coupon = Coupon.builder()
                .id(1L)
                .code("SALE10")
                .discountType(Coupon.DiscountType.PERCENTAGE)
                .discountValue(BigDecimal.TEN)
                .isActive(true)
                .build();
        when(couponService.getAllCoupons()).thenReturn(List.of(coupon));

        mockMvc.perform(get("/api/coupons/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].code").value("SALE10"))
                .andExpect(jsonPath("$[0].discountType").value("PERCENTAGE"))
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    @WithMockUser(username = "admin@tmdt.com", authorities = "ROLE_ADMIN")
    @DisplayName("PATCH /api/coupons/admin/{id}/status trả VALIDATION_ERROR khi thiếu isActive")
    void toggleStatus_ReturnsValidationErrorWhenMissingIsActive() throws Exception {
        mockMvc.perform(patch("/api/coupons/admin/1/status")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.isActive").exists());

        verify(couponService, never()).toggleCouponStatus(anyLong(), anyBoolean());
    }
}
