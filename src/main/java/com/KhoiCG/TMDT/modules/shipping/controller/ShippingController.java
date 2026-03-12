package com.KhoiCG.TMDT.modules.shipping.controller;

import com.KhoiCG.TMDT.modules.shipping.dto.CreateShippingForOrderRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeRequest;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingFeeResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingLogResponse;
import com.KhoiCG.TMDT.modules.shipping.dto.ShippingOrderResponse;
import com.KhoiCG.TMDT.modules.shipping.service.ShippingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shipping")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "shipping", name = "enabled", havingValue = "true", matchIfMissing = false)
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/fee")
    public ResponseEntity<ShippingFeeResponse> calculateFee(@Valid @RequestBody ShippingFeeRequest request) {
        return ResponseEntity.ok(shippingService.calculateFee(request));
    }

    @PostMapping("/orders/{orderId}/create")
    public ResponseEntity<ShippingOrderResponse> createForOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody CreateShippingForOrderRequest request
    ) {
        return ResponseEntity.ok(shippingService.createShippingForOrder(orderId, request));
    }

    @GetMapping("/orders/{orderId}/logs")
    public ResponseEntity<List<ShippingLogResponse>> getLogs(@PathVariable Long orderId) {
        return ResponseEntity.ok(shippingService.getLogsByOrderId(orderId));
    }
}

