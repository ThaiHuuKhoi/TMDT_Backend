package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.modules.auth.entity.UserPrincipal;
import com.KhoiCG.TMDT.modules.payment.dto.CreateSessionRequest;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final StripeService stripeService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CreateSessionRequest request) {
        try {
            // Lấy thông tin user từ Token (JWT)
            if (SecurityContextHolder.getContext().getAuthentication() == null ||
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
            }

            UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long userId = userDetails.getUser().getId();

            // 🔥 TRUYỀN userId THAY VÌ request.getItems()
            String clientSecret = stripeService.createCheckoutSession(userId, request.getCouponCode());

            return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi server: " + e.getMessage()));
        }
    }

    @GetMapping("/{session_id}")
    public ResponseEntity<?> getSessionStatus(@PathVariable("session_id") String sessionId) {
        try {
            Session session = stripeService.retrieveSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "status", session.getStatus(),
                    "paymentStatus", session.getPaymentStatus()
            ));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body(Map.of("message", e.getMessage()));
        }
    }
}