package com.KhoiCG.TMDT.paymentService.controler;

import com.KhoiCG.TMDT.paymentService.dto.CreateSessionRequest;
import com.KhoiCG.TMDT.paymentService.service.StripeService;
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
            // Lấy userId (nếu cần dùng sau này)
            String userId = "anonymous";
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                userId = SecurityContextHolder.getContext().getAuthentication().getName();
            }

            // 👇 SỬA DÒNG NÀY:
            // Cũ (Sai): stripeService.createCheckoutSession(userId, request.getCart());
            // Mới (Đúng): Truyền List Items trước, Coupon Code sau
            String clientSecret = stripeService.createCheckoutSession(
                    request.getItems(),
                    request.getCouponCode()
            );

            return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /sessions/{session_id}
    @GetMapping("/{session_id}")
    public ResponseEntity<?> getSessionStatus(@PathVariable("session_id") String sessionId) {
        try {
            Session session = stripeService.retrieveSession(sessionId);
            return ResponseEntity.ok(Map.of(
                    "status", session.getStatus(),
                    "paymentStatus", session.getPaymentStatus()
            ));
        } catch (StripeException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}