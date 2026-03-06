package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.product.dto.ReviewRequest;
import com.KhoiCG.TMDT.modules.product.dto.ReviewResponseDto;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.service.ReviewService;
import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // GET: Lấy danh sách review
    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // POST: Viết review (Yêu cầu đăng nhập)
    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody ReviewRequest request) {
        if (request.getRating() < 1 || request.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }

        // Lấy thông tin user đang đăng nhập từ Spring Security
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();

        return ResponseEntity.ok(reviewService.createReview(userId, request.getProductId(), request.getRating(), request.getComment()));
    }
}