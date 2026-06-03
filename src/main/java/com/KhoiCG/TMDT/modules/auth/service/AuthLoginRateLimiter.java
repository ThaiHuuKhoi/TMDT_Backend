package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthLoginRateLimiter extends RedisRateLimiterBase {

    @Value("${application.security.login.rate-limit.ip-per-15-min:30}")
    private int maxAttemptsPerIpPer15Min;

    @Value("${application.security.login.rate-limit.email-per-15-min:10}")
    private int maxAttemptsPerEmailPer15Min;

    @Value("${application.security.login.lockout.max-failures:5}")
    private int maxFailuresBeforeLockout;

    @Value("${application.security.login.lockout.window-minutes:15}")
    private int lockoutWindowMinutes;

    public AuthLoginRateLimiter(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    public void assertLoginAllowed(String email, String clientIp) {
        if (isLockedOut(email)) {
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "LOGIN_LOCKED",
                    "Quá nhiều lần đăng nhập sai. Vui lòng thử lại sau " + lockoutWindowMinutes + " phút."
            );
        }
        if (clientIp != null && !clientIp.isBlank()) {
            bumpLimit("auth:rl:login:ip:" + clientIp.trim(), maxAttemptsPerIpPer15Min, Duration.ofMinutes(15));
        }
        bumpLimit("auth:rl:login:email:" + normalizeEmail(email), maxAttemptsPerEmailPer15Min, Duration.ofMinutes(15));
    }

    public void onLoginFailure(String email) {
        String key = "auth:login:fail:" + normalizeEmail(email);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(lockoutWindowMinutes));
        }
    }

    public void onLoginSuccess(String email) {
        stringRedisTemplate.delete("auth:login:fail:" + normalizeEmail(email));
    }

    private boolean isLockedOut(String email) {
        String val = stringRedisTemplate.opsForValue().get("auth:login:fail:" + normalizeEmail(email));
        if (val == null) return false;
        try {
            return Long.parseLong(val) >= maxFailuresBeforeLockout;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
