package com.KhoiCG.TMDT.modules.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FeatureFlagService {

    private static final String OTP_REGISTRATION_KEY = "feature:otp-registration";
    private final StringRedisTemplate stringRedisTemplate;

    public boolean isOtpRegistrationEnabled() {
        String val = stringRedisTemplate.opsForValue().get(OTP_REGISTRATION_KEY);
        return !"false".equals(val);
    }

    public void setOtpRegistrationEnabled(boolean enabled) {
        stringRedisTemplate.opsForValue().set(OTP_REGISTRATION_KEY, String.valueOf(enabled));
    }
}
