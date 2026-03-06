package com.KhoiCG.TMDT.modules.order.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.order.dto.CartRequest;
import com.KhoiCG.TMDT.modules.order.entity.Cart;
import com.KhoiCG.TMDT.modules.order.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    private Long getCurrentUserId() {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return userDetails.getUser().getId();
    }

    @GetMapping
    public ResponseEntity<Cart> getMyCart() {
        return ResponseEntity.ok(cartService.getOrCreateCart(getCurrentUserId()));
    }

    @PostMapping("/items")
    public ResponseEntity<?> addToCart(@RequestBody CartRequest request) {
        try {
            Cart cart = cartService.addToCart(getCurrentUserId(), request.getVariantId(), request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/items")
    public ResponseEntity<?> updateQuantity(@RequestBody CartRequest request) {
        try {
            Cart cart = cartService.updateItemQuantity(getCurrentUserId(), request.getVariantId(), request.getQuantity());
            return ResponseEntity.ok(cart);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/items/{variantId}")
    public ResponseEntity<Cart> removeItem(@PathVariable Long variantId) {
        return ResponseEntity.ok(cartService.removeItem(getCurrentUserId(), variantId));
    }

    @DeleteMapping
    public ResponseEntity<String> clearCart() {
        cartService.clearCart(getCurrentUserId());
        return ResponseEntity.ok("Đã làm trống giỏ hàng");
    }
}