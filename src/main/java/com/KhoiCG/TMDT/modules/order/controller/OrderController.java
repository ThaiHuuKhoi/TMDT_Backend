package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.order.dto.AdminDashboardStatsResponse;
import com.KhoiCG.TMDT.modules.order.dto.MarketRecommendationResponse;
import com.KhoiCG.TMDT.modules.order.dto.AdminOrderCreateRequest;
import com.KhoiCG.TMDT.modules.order.dto.OrderBulkIdsRequest;
import com.KhoiCG.TMDT.modules.order.dto.OrderChartResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderPageResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderResponse;
import com.KhoiCG.TMDT.modules.order.dto.OrderStatusUpdateRequest;
import com.KhoiCG.TMDT.modules.order.mapper.OrderMapper;
import com.KhoiCG.TMDT.modules.order.service.OrderService;
import com.KhoiCG.TMDT.modules.order.service.MarketRecommendationService;
import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final MarketRecommendationService marketRecommendationService;
    private final OrderMapper orderMapper;

    @GetMapping("/user-orders")
    public ResponseEntity<List<OrderResponse>> getUserOrders() {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        return ResponseEntity.ok(orderService.getUserOrders(userId));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderPageResponse> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return new ResponseEntity<>(orderService.getAllOrdersPageForAdmin(page, size), HttpStatus.OK);
    }

    @GetMapping("/chart")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderChartResponse>> getOrderChart() {
        List<OrderChartResponse> orderChartResponse = orderService.getOrderChart();
        return new ResponseEntity<>(orderChartResponse, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrderDetails(@PathVariable Long id) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        OrderResponse orderResponse = orderService.getOrderDetails(id, userId);
        return ResponseEntity.ok(orderResponse);
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<List<OrderResponse>> getAllOrdersForAdmin(
            @RequestParam(required = false) Integer limit) {
        return ResponseEntity.ok(orderService.getAllOrdersForAdmin(limit));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<AdminDashboardStatsResponse> getAdminDashboardStats() {
        return ResponseEntity.ok(orderService.getAdminDashboardStats());
    }

    @GetMapping("/admin/market-recommendations")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<MarketRecommendationResponse> getMarketRecommendations(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "14") int horizonDays) {
        return ResponseEntity.ok(marketRecommendationService.getRecommendations(limit, horizonDays));
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> getOrderForAdmin(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForAdmin(id));
    }

    @PostMapping("/admin")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> createOrderByAdmin(@Valid @RequestBody AdminOrderCreateRequest request) {
        return new ResponseEntity<>(orderService.createOrderByAdmin(request), HttpStatus.CREATED);
    }

    @PostMapping("/admin/bulk-cancel")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<Void> bulkCancelOrders(@Valid @RequestBody OrderBulkIdsRequest request) {
        orderService.bulkCancelOrdersForAdmin(request.getIds());
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(orderMapper.toOrderResponse(orderService.updateOrderStatus(id, request.getStatus())));
    }
}