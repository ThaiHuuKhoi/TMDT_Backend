package com.KhoiCG.TMDT.modules.chatbot.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatRequest;
import com.KhoiCG.TMDT.modules.chatbot.dto.ChatResponse;
import com.KhoiCG.TMDT.modules.chatbot.service.ChatbotRateLimiter;
import com.KhoiCG.TMDT.modules.chatbot.service.ChatbotService;
import com.KhoiCG.TMDT.modules.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
public class ChatbotController {

	private final ChatbotService chatbotService;
	private final ChatbotRateLimiter chatbotRateLimiter;
	@Value("${application.security.trust-forward-headers:false}")
	private boolean trustForwardHeaders;

	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(
			@Valid @RequestBody ChatRequest request,
			HttpServletRequest httpRequest) {
		User currentUser = resolveCurrentUser();
		String ip = clientIp(httpRequest, trustForwardHeaders);
		chatbotRateLimiter.assertAllowed(ip, currentUser != null ? currentUser.getId() : null);
		return ResponseEntity.ok(chatbotService.chat(request, currentUser));
	}

	private User resolveCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !auth.isAuthenticated()) return null;
		if (auth.getPrincipal() instanceof UserPrincipal up) {
			return up.getUser();
		}
		return null;
	}

	private static String clientIp(HttpServletRequest request, boolean trustForwardHeaders) {
		if (trustForwardHeaders) {
			String xff = request.getHeader("X-Forwarded-For");
			if (xff != null && !xff.isBlank()) {
				return xff.split(",")[0].trim();
			}
		}
		String remote = request.getRemoteAddr();
		return remote != null ? remote : "";
	}
}
