package com.KhoiCG.TMDT.orderService.controller;

import com.KhoiCG.TMDT.orderService.dto.CheckoutRequest;
import com.KhoiCG.TMDT.orderService.dto.OrderChartResponse;
import com.KhoiCG.TMDT.orderService.entity.Order;
import com.KhoiCG.TMDT.orderService.service.OrderService;
import com.KhoiCG.TMDT.paymentService.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/orders") // Tiền tố URL
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final StripeService stripeService;

    @PostMapping("/create-from-stripe")
    public ResponseEntity<?> createOrderFromStripe(@RequestParam String sessionId) {
        try {
            Order order = orderService.createOrderFromStripe(sessionId);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            // Nếu lỗi là "Order already exists" thì vẫn trả về OK để Frontend clear cart
            if (e.getMessage().contains("exists")) {
                return ResponseEntity.ok("Order already processed");
            }
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // Node: fastify.get("/user-orders", { preHandler: shouldBeUser })
    @GetMapping("/user-orders")
    public ResponseEntity<List<Order>> getUserOrders() {
        // Lấy userId từ SecurityContext (đã setup ở bước Auth)
        // Cách lấy này phụ thuộc vào cách bạn config JWT Filter.
        // Ví dụ đơn giản nhất là lấy name/subject từ token.
        String userId = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication().getName();

        return new ResponseEntity<>(orderService.getUserOrders(userId),HttpStatus.OK);
    }

    // Node: fastify.get("/orders", { preHandler: shouldBeAdmin })
    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN')") // Hoặc hasRole('ADMIN') tùy config
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(defaultValue = "10") int limit) {
        return new ResponseEntity<>(orderService.getAllOrders(limit),HttpStatus.OK);
    }

    // Node: fastify.get("/order-chart", { preHandler: shouldBeAdmin })
    @GetMapping("/chart")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<OrderChartResponse>> getOrderChart() {
        return new ResponseEntity<>(orderService.getOrderChart(), HttpStatus.OK);
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutRequest request) {
        try {
            // Gọi StripeService với list items và couponCode
            String clientSecret = stripeService.createCheckoutSession(
                    request.getItems(),
                    request.getCouponCode()
            );

            // Trả về JSON chứa clientSecret
            // Frontend đang chờ: { clientSecret: "..." }
            return ResponseEntity.ok(java.util.Map.of("clientSecret", clientSecret));

        } catch (StripeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Lỗi tạo thanh toán: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}