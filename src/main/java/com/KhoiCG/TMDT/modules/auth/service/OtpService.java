package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.modules.auth.dto.PendingRegistration;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String KEY_PREFIX = "otp:register:";
    private static final Duration OTP_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public String generateAndStore(PendingRegistration pending) {
        String otp = String.format("%06d", new SecureRandom().nextInt(1_000_000));
        pending.setOtp(otp);
        try {
            String json = objectMapper.writeValueAsString(pending);
            redisTemplate.opsForValue().set(KEY_PREFIX + pending.getEmail(), json, OTP_TTL);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi lưu OTP", e);
        }
        return otp;
    }

    public PendingRegistration verifyAndConsume(String email, String otp) {
        String key = KEY_PREFIX + email;
        String json = redisTemplate.opsForValue().get(key);

        if (json == null) {
            throw new RuntimeException("OTP đã hết hạn hoặc không tồn tại");
        }

        PendingRegistration pending;
        try {
            pending = objectMapper.readValue(json, PendingRegistration.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi đọc OTP", e);
        }

        if (!pending.getOtp().equals(otp)) {
            throw new RuntimeException("OTP không hợp lệ");
        }

        redisTemplate.delete(key);
        return pending;
    }
}
