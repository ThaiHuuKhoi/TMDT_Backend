package com.KhoiCG.TMDT.modules.product.controller;

import com.KhoiCG.TMDT.modules.product.dto.ReviewRequest;
import jakarta.validation.Valid;
import com.KhoiCG.TMDT.modules.product.dto.ReviewResponseDto;
import com.KhoiCG.TMDT.modules.product.service.ReviewService;
import com.KhoiCG.TMDT.modules.auth.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/{productId}")
    public ResponseEntity<List<ReviewResponseDto>> getProductReviews(@PathVariable Long productId) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponseDto> createReview(@RequestBody @Valid ReviewRequest request) {
        UserPrincipal userDetails = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userDetails.getUser().getId();
        return ResponseEntity.ok(reviewService.createReview(userId, request.getProductId(), request.getRating(), request.getComment()));
    }
}