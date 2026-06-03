package com.KhoiCG.TMDT.modules.chatbot.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ChatbotRateLimiter {

	private final StringRedisTemplate stringRedisTemplate;

	@Value("${application.chatbot.rate-limit.ip-per-minute:30}")
	private int maxChatPerIpPerMinute;

	@Value("${application.chatbot.rate-limit.user-per-minute:60}")
	private int maxChatPerUserPerMinute;

	public void assertAllowed(String clientIp, Long userId) {
		if (clientIp != null && !clientIp.isBlank()) {
			bumpLimit("chat:rl:ip:" + clientIp.trim(), maxChatPerIpPerMinute, Duration.ofMinutes(1));
		}
		if (userId != null) {
			bumpLimit("chat:rl:user:" + userId, maxChatPerUserPerMinute, Duration.ofMinutes(1));
		}
	}

	private void bumpLimit(String redisKey, int maxPerWindow, Duration window) {
		Long n;
		try {
			n = stringRedisTemplate.opsForValue().increment(redisKey);
		} catch (Exception e) {
			// Redis unavailable — fail open rather than blocking all chat traffic
			return;
		}
		if (n == null) return;

		if (n == 1L) {
			try {
				stringRedisTemplate.expire(redisKey, window);
			} catch (Exception ignored) {
				// TTL set failure is handled defensively below on subsequent requests
			}
		} else {
			// Defensive: if a prior expire call failed the key has no TTL (-1), reset it now
			try {
				Long ttl = stringRedisTemplate.getExpire(redisKey);
				if (ttl != null && ttl < 0) {
					stringRedisTemplate.expire(redisKey, window);
				}
			} catch (Exception ignored) {
			}
		}

		if (n > maxPerWindow) {
			throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "CHATBOT_RATE_LIMITED",
					"Quá nhiều tin nhắn. Vui lòng thử lại sau.");
		}
	}
}
