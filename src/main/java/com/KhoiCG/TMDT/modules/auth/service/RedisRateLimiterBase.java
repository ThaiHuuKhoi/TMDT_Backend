package com.KhoiCG.TMDT.modules.auth.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;

import java.time.Duration;

public abstract class RedisRateLimiterBase {

    protected final StringRedisTemplate stringRedisTemplate;

    protected RedisRateLimiterBase(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    protected void bumpLimit(String redisKey, int maxPerWindow, Duration window) {
        Long n = stringRedisTemplate.opsForValue().increment(redisKey);
        if (n != null && n == 1L) {
            stringRedisTemplate.expire(redisKey, window);
        }
        if (n != null && n > maxPerWindow) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_REQUESTS", "Quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }
    }

    protected static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
