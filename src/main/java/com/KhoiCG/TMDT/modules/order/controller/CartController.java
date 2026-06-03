package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.dto.CartRequest;
import com.KhoiCG.TMDT.modules.order.dto.CartSummaryResponse;
import com.KhoiCG.TMDT.modules.order.mapper.CartMapper;
import com.KhoiCG.TMDT.modules.order.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final CartMapper cartMapper;

    private Long getCurrentUserId() {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser().getId();
    }

    @GetMapping
    public ResponseEntity<CartSummaryResponse> getMyCart() {
        return ResponseEntity.ok(cartMapper.toCartSummary(cartService.getOrCreateCart(getCurrentUserId())));
    }

    @PostMapping("/items")
    public ResponseEntity<CartSummaryResponse> addToCart(@Valid @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartMapper.toCartSummary(
                cartService.addToCart(getCurrentUserId(), request.getVariantId(), request.getQuantity())
        ));
    }

    @PutMapping("/items")
    public ResponseEntity<CartSummaryResponse> updateQuantity(@Valid @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartMapper.toCartSummary(
                cartService.updateItemQuantity(getCurrentUserId(), request.getVariantId(), request.getQuantity())
        ));
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<CartSummaryResponse> removeItem(@PathVariable Long variantId) {
        return ResponseEntity.ok(cartMapper.toCartSummary(cartService.removeItem(getCurrentUserId(), variantId)));
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart() {
        cartService.clearCart(getCurrentUserId());
        return ResponseEntity.ok("Đã làm trống giỏ hàng");
    }
}