package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // GET: Lấy danh sách review (Public - Ai cũng xem được)
    @GetMapping("/{productId}")
    public ResponseEntity<List<Review>> getProductReviews(@PathVariable String productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    // POST: Viết review (Cần đăng nhập - Token xử lý ở Filter/Gateway)
    @PostMapping
    public ResponseEntity<Review> createReview(@RequestBody Review review) {
        // Validate cơ bản
        if (review.getRating() < 1 || review.getRating() > 5) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(reviewService.createReview(review));
    }
}