package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AuthRegistrationRateLimiter extends RedisRateLimiterBase {

    private static final String REG_OTP_KEY_PREFIX = "auth:reg-otp:";

    @Value("${application.security.registration.rate-limit.email-per-hour:5}")
    private int maxRegisterPerEmailPerHour;

    @Value("${application.security.registration.rate-limit.ip-per-hour:30}")
    private int maxRegisterPerIpPerHour;

    @Value("${application.security.registration.rate-limit.verify-ip-per-hour:80}")
    private int maxVerifyOtpPerIpPerHour;

    @Value("${application.security.password-reset.rate-limit.email-per-hour:5}")
    private int maxForgotPasswordPerEmailPerHour;

    @Value("${application.security.password-reset.rate-limit.ip-per-hour:20}")
    private int maxForgotPasswordPerIpPerHour;

    @Value("${application.security.password-reset.rate-limit.reset-ip-per-hour:40}")
    private int maxResetPasswordPerIpPerHour;

    @Value("${application.security.registration.otp.max-failed-attempts:5}")
    private int maxOtpFailedAttempts;

    @Value("${application.security.registration.otp.failed-window-minutes:15}")
    private int otpFailedWindowMinutes;

    public AuthRegistrationRateLimiter(StringRedisTemplate stringRedisTemplate) {
        super(stringRedisTemplate);
    }

    public void assertRegisterAllowed(String email, String clientIp) {
        bumpLimit("auth:rl:reg:email:" + normalizeEmail(email), maxRegisterPerEmailPerHour, Duration.ofHours(1));
        if (clientIp != null && !clientIp.isBlank()) {
            bumpLimit("auth:rl:reg:ip:" + clientIp.trim(), maxRegisterPerIpPerHour, Duration.ofHours(1));
        }
    }

    public void assertVerifyOtpAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return;
        bumpLimit("auth:rl:verify-otp:ip:" + clientIp.trim(), maxVerifyOtpPerIpPerHour, Duration.ofHours(1));
    }

    public void assertForgotPasswordAllowed(String email, String clientIp) {
        bumpLimit("auth:rl:forgot-pw:email:" + normalizeEmail(email), maxForgotPasswordPerEmailPerHour, Duration.ofHours(1));
        if (clientIp != null && !clientIp.isBlank()) {
            bumpLimit("auth:rl:forgot-pw:ip:" + clientIp.trim(), maxForgotPasswordPerIpPerHour, Duration.ofHours(1));
        }
    }

    public void assertResetPasswordAllowed(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) return;
        bumpLimit("auth:rl:reset-pw:ip:" + clientIp.trim(), maxResetPasswordPerIpPerHour, Duration.ofHours(1));
    }

    public void assertChangePasswordAllowed(String email, String clientIp) {
        bumpLimit("auth:rl:change-pw:email:" + normalizeEmail(email), 5, Duration.ofMinutes(15));
        if (clientIp != null && !clientIp.isBlank()) {
            bumpLimit("auth:rl:change-pw:ip:" + clientIp.trim(), 20, Duration.ofMinutes(15));
        }
    }

    public void onOtpMismatch(String email) {
        String key = "auth:reg:otp-fail:" + normalizeEmail(email);
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            stringRedisTemplate.expire(key, Duration.ofMinutes(otpFailedWindowMinutes));
        }
        if (count != null && count >= maxOtpFailedAttempts) {
            stringRedisTemplate.delete(REG_OTP_KEY_PREFIX + normalizeEmail(email));
            stringRedisTemplate.delete(key);
            throw new ApiException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "OTP_ATTEMPTS_EXCEEDED",
                    "Quá nhiều lần nhập sai OTP. Vui lòng đăng ký lại để nhận mã mới."
            );
        }
    }

    public void clearOtpFailures(String email) {
        stringRedisTemplate.delete("auth:reg:otp-fail:" + normalizeEmail(email));
    }
}
