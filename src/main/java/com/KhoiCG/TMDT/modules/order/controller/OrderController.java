package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.order.dto.CheckoutRequest;
import com.KhoiCG.TMDT.modules.order.dto.OrderChartResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/create-from-stripe")
    public ResponseEntity<?> createOrderFromStripe(@RequestParam String sessionId) {
        try {
            Order order = orderService.confirmOrderPayment(sessionId, Payment.PaymentMethod.STRIPE);
            return ResponseEntity.ok(order);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // Đã XÓA: API /create-checkout-session ở đây.
    // Frontend từ nay sẽ gọi POST /api/sessions/create-checkout-session (bên module payment) để lấy link Stripe nhé!

    @GetMapping("/user-orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(defaultValue = "10") int limit) {
        return new ResponseEntity<>(orderService.getAllOrders(limit), HttpStatus.OK);
    }

    @GetMapping("/chart")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderChartResponse>> getOrderChart() {
        List<OrderChartResponse> orderChartResponse = orderService.getOrderChart();
        return new ResponseEntity<>(orderChartResponse, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long id) {
        try {
            UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            Long userId = userDetails.getUser().getId();

            OrderResponse orderResponse = orderService.getOrderDetails(id, userId);
            return ResponseEntity.ok(orderResponse);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForAdmin() {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin());
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<?> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        Order order = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(order);
    }
}