package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;

    // 1. Tạo đánh giá mới
    public Review createReview(Review review) {
        // Set thời gian hiện tại
        review.setCreatedAt(LocalDateTime.now());

        // Lưu review vào DB
        Review savedReview = reviewRepository.save(review);

        // 🔥 QUAN TRỌNG: Tính lại điểm trung bình cho Product ngay lập tức
        updateProductRating(review.getProductId());

        return savedReview;
    }

    // 2. Lấy danh sách đánh giá
    public List<Review> getReviewsByProduct(String productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    // --- Hàm phụ: Tính toán rating ---
    private void updateProductRating(String productId) {
        // Tìm sản phẩm
        Product product = productRepository.findById(Long.valueOf(productId))
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Lấy tất cả review của sản phẩm này
        List<Review> reviews = reviewRepository.findByProductId(productId);

        if (reviews.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            // Tính trung bình cộng
            double average = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);

            // Làm tròn 1 chữ số thập phân (VD: 4.56 -> 4.6)
            double roundedAverage = Math.round(average * 10.0) / 10.0;

            product.setAverageRating(roundedAverage);
            product.setReviewCount(reviews.size());
        }

        // Cập nhật lại Product
        productRepository.save(product);
    }
}