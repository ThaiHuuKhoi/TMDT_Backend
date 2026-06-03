package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.common.config.JwtFilter;
import com.KhoiCG.TMDT.common.exception.GlobalExceptionHandler;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.dto.ShippingDetailsRequest;
import com.KhoiCG.TMDT.modules.payment.service.VNPayService;
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

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = VNPayController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtFilter.class),
        properties = {
                "spring.cache.type=simple",
                "spring.security.oauth2.client.registration.google.client-id=test-google-id",
                "spring.security.oauth2.client.registration.google.client-secret=test-google-secret",
                "spring.security.oauth2.client.registration.facebook.client-id=test-facebook-id",
                "spring.security.oauth2.client.registration.facebook.client-secret=test-facebook-secret"
        }
)
@Import({GlobalExceptionHandler.class, VNPayControllerWebMvcTest.TestSecurityConfig.class})
class VNPayControllerWebMvcTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VNPayService vnPayService;

    @MockitoBean
    private OrderService orderService;

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
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/api/vnpay/ipn", "/api/vnpay/return").permitAll()
                            .anyRequest().authenticated())
                    .build();
        }
    }

    private Map<String, Object> validShippingMap() {
        return Map.of(
                "name", "Nguyen A",
                "email", "a@b.com",
                "phone", "0912345678",
                "address", "1 Street",
                "city", "HN"
        );
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/vnpay/create-payment trả VALIDATION_ERROR khi couponCode quá dài")
    void createPayment_ReturnsValidationErrorWhenCouponTooLong() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of(
                "couponCode", "A".repeat(51),
                "shipping", validShippingMap()
        ));
        mockMvc.perform(post("/api/vnpay/create-payment")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors.couponCode").exists());

        verify(orderService, never()).createPendingOrder(anyLong(), anyString(), any(), any(ShippingDetailsRequest.class));
        verify(vnPayService, never()).createOrder(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "user@tmdt.com", authorities = "ROLE_USER")
    @DisplayName("POST /api/vnpay/create-payment trả VALIDATION_ERROR khi thiếu shipping")
    void createPayment_ReturnsValidationErrorWhenShippingMissing() throws Exception {
        String payload = objectMapper.writeValueAsString(Map.of("couponCode", ""));
        mockMvc.perform(post("/api/vnpay/create-payment")
                        .contentType("application/json")
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        verify(orderService, never()).createPendingOrder(anyLong(), anyString(), any(), any(ShippingDetailsRequest.class));
    }

    @Test
    @DisplayName("GET /api/vnpay/ipn trả success khi checksum hợp lệ và giao dịch thành công")
    void ipn_ReturnsSuccessWhenChecksumValidAndResponseCode00() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(true);

        mockMvc.perform(get("/api/vnpay/ipn")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNPay_abc123")
                        .param("vnp_SecureHash", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("00"));

        verify(orderService, times(1)).confirmOrderPayment(eq("VNPay_abc123"), eq(com.KhoiCG.TMDT.modules.payment.entity.Payment.PaymentMethod.VNPAY));
    }

    @Test
    @DisplayName("GET /api/vnpay/ipn trả INVALID_VNPAY_IPN khi thiếu txnRef")
    void ipn_ReturnsErrorWhenMissingTxnRef() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(true);

        mockMvc.perform(get("/api/vnpay/ipn")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", "dummy"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_VNPAY_IPN"));

        verify(orderService, never()).confirmOrderPayment(anyString(), any());
    }

    @Test
    @DisplayName("GET /api/vnpay/ipn trả RspCode=97 khi checksum không hợp lệ")
    void ipn_ReturnsInvalidChecksumWhenSignatureFails() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(false);

        mockMvc.perform(get("/api/vnpay/ipn")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNPay_bad")
                        .param("vnp_SecureHash", "invalid"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.RspCode").value("97"));

        verify(orderService, never()).confirmOrderPayment(anyString(), any());
    }

    @Test
    @DisplayName("GET /api/vnpay/return xác nhận đơn khi chữ ký hợp lệ và mã 00")
    void return_ConfirmsWhenSignatureValidAnd00() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(true);

        mockMvc.perform(get("/api/vnpay/return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNPay_xyz")
                        .param("vnp_SecureHash", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.txnRef").value("VNPay_xyz"));

        verify(orderService, times(1)).confirmOrderPayment(eq("VNPay_xyz"), eq(com.KhoiCG.TMDT.modules.payment.entity.Payment.PaymentMethod.VNPAY));
    }

    @Test
    @DisplayName("GET /api/vnpay/return 400 khi chữ ký không hợp lệ")
    void return_BadRequestWhenSignatureInvalid() throws Exception {
        when(vnPayService.verifySignature(any())).thenReturn(false);

        mockMvc.perform(get("/api/vnpay/return")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TxnRef", "VNPay_xyz"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_SIGNATURE"));

        verify(orderService, never()).confirmOrderPayment(anyString(), any());
    }
}
