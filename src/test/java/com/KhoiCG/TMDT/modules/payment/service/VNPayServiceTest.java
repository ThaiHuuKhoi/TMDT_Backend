package com.KhoiCG.TMDT.modules.payment.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VNPayServiceTest {
    private VNPayService vnPayService;

    @BeforeEach
    void setUp() {
        vnPayService = new VNPayService();
        ReflectionTestUtils.setField(vnPayService, "vnpTmnCode", "TMNCODE123");
        ReflectionTestUtils.setField(vnPayService, "vnpHashSecret", "SECRET_KEY_123");
        ReflectionTestUtils.setField(vnPayService, "vnpPayUrl", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html");
        ReflectionTestUtils.setField(vnPayService, "vnpReturnUrl", "https://example.com/vnpay-return");
    }

    @Test
    @DisplayName("createOrder: URL chứa IP đầu vào thay vì hard-code")
    void createOrder_UsesProvidedIpAddress() {
        String url = vnPayService.createOrder(100_000L, "Thanh toan", "TXN_001", "10.10.10.10");
        assertTrue(url.contains("vnp_IpAddr=10.10.10.10"));
        assertTrue(url.contains("vnp_SecureHash="));
    }

    @Test
    @DisplayName("verifySignature: Không mutate map đầu vào")
    void verifySignature_DoesNotMutateInputMap() {
        String url = vnPayService.createOrder(100_000L, "Thanh toan", "TXN_002", "10.0.0.2");
        String query = url.substring(url.indexOf('?') + 1);
        Map<String, String> fields = new LinkedHashMap<>();
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            String key = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String value = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            fields.put(key, value);
        }
        Map<String, String> original = new HashMap<>(fields);

        boolean result = vnPayService.verifySignature(fields);

        assertTrue(result);
        assertEquals(original, fields);
    }
}
