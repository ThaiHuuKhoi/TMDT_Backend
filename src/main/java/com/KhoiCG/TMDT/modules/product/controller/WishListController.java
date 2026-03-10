package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import com.KhoiCG.TMDT.modules.product.service.WishListService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlists")
@RequiredArgsConstructor
public class WishListController {

    private final WishListService wishListService;

    @PostMapping("/{productId}")
    public ResponseEntity<String> toggleWishlist(@PathVariable Long productId) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        String resultMessage = wishListService.toggleWishlist(userId, productId);
        return ResponseEntity.ok(resultMessage);
    }

    @GetMapping
    public ResponseEntity<?> getMyWishlist(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        return ResponseEntity.ok(wishListService.getUserWishlist(userId, page, size));
    }
}