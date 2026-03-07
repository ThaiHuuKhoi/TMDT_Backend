package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.modules.product.dto.ReviewResponseDto;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.event.ReviewCreatedEvent;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher eventPublisher;

    // 1. TẠO ĐÁNH GIÁ MỚI: Xóa cache danh sách đánh giá của sản phẩm này
    @CacheEvict(value = "product_reviews", key = "#productId")
    @Transactional
    public Review createReview(Long userId, Long productId, Integer rating, String comment) {

        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new RuntimeException("Bạn đã đánh giá sản phẩm này rồi!");
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("Người dùng không tồn tại"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(rating)
                .comment(comment)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Review của sản phẩm ID: {} vì có đánh giá mới", productId);

        // 2. Phát sự kiện để tính lại điểm trung bình
        eventPublisher.publishEvent(new ReviewCreatedEvent(productId));

        return savedReview;
    }

    // 2. LẤY DANH SÁCH ĐÁNH GIÁ: Lưu vào Cache
    @Cacheable(value = "product_reviews", key = "#productId")
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        log.info("🚀 [CACHE MISS] - Đang query Database để lấy danh sách Review của sản phẩm ID: {}", productId);
        List<Review> reviews = reviewRepository.findByProductId(productId);

        return reviews.stream().map(r -> {
            ReviewResponseDto dto = new ReviewResponseDto();
            dto.setId(r.getId());
            dto.setUserName(r.getUser().getName()); // Lưu ý: Hàm này có thể gây ra lỗi N+1 Query nếu không Fetch/Join User
            dto.setRating(r.getRating());
            dto.setComment(r.getComment());
            dto.setCreatedAt(r.getCreatedAt());
            return dto;
        }).toList();
    }
}