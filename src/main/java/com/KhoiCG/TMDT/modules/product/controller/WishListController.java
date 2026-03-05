package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.auth.entity.UserPrincipal;
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

    // POST: Thêm hoặc Xóa sản phẩm khỏi danh sách yêu thích (Toggle)
    @PostMapping("/{productId}")
    public ResponseEntity<String> toggleWishlist(@PathVariable Long productId) {
        // Lấy thông tin user từ context bảo mật (JWT Token)
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        String resultMessage = wishListService.toggleWishlist(userId, productId);
        return ResponseEntity.ok(resultMessage);
    }

    // GET: Lấy danh sách sản phẩm yêu thích của user hiện tại (có phân trang)
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