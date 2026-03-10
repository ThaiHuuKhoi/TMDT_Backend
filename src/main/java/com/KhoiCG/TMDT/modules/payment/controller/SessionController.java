package com.KhoiCG.TMDT.modules.payment.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.dto.CreateSessionRequest;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final StripeService stripeService;
    private final OrderService orderService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CreateSessionRequest request) {
        try {
            if (SecurityContextHolder.getContext().getAuthentication() == null ||
                    SecurityContextHolder.getContext().getAuthentication().getPrincipal().equals("anonymousUser")) {
                return ResponseEntity.status(401).body(Map.of("message", "Vui lòng đăng nhập"));
            }

            UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long userId = userDetails.getUser().getId();

            String txnRef = "STRIPE_" + UUID.randomUUID().toString().substring(0, 8);

            Order pendingOrder = orderService.createPendingOrder(userId, txnRef, request.getCouponCode());

            String clientSecret = stripeService.createCheckoutSession(
                    userId,
                    pendingOrder.getTotalAmount().longValue(),
                    "Thanh toán đơn hàng " + txnRef,
                    txnRef
            );

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