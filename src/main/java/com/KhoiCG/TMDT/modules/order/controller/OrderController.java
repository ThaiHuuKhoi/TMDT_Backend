package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.order.dto.CheckoutRequest;
import com.KhoiCG.TMDT.modules.order.dto.OrderChartResponse;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.payment.service.StripeService;
import com.stripe.exception.StripeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<Order>> getAllOrders(@RequestParam(defaultValue = "10") int limit) {
        return new ResponseEntity<>(orderService.getAllOrders(limit),HttpStatus.OK);
    }

    // Node: fastify.get("/order-chart", { preHandler: shouldBeAdmin })
    @GetMapping("/chart")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<List<OrderChartResponse>> getOrderChart() {
        List<OrderChartResponse> orderChartResponse = orderService.getOrderChart();
        System.out.println(orderChartResponse);
        return new ResponseEntity<>(orderChartResponse, HttpStatus.OK);
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
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrderDetails(@PathVariable String id) {
        Order order = orderService.getOrderDeTails(id);
        if (order == null) {
            return ResponseEntity.status(404).body("Không tìm thấy đơn hàng hoặc bạn không có quyền truy cập.");
        }
        return ResponseEntity.ok(order);
    }

    @GetMapping("/admin/all")
    public ResponseEntity<List<Order>> getAllOrdersForAdmin() {
        List<Order> orders = orderService.getAllOrdersForAdmin();
        return ResponseEntity.ok(orders);
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<?> updateOrderStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        String newStatus = body.get("status");
        Order order = orderService.updateOrderStatus(id, newStatus);
        return ResponseEntity.ok(order);
    }

}