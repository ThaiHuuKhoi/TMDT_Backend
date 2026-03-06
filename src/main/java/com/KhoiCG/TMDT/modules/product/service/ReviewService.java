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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepo userRepo;

    private final ApplicationEventPublisher eventPublisher;

    // 1. Tạo đánh giá mới
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

        // 2. Phát sự kiện thay vì gọi trực tiếp hàm update
        eventPublisher.publishEvent(new ReviewCreatedEvent(productId));

        return savedReview;
    }

    // 2. Lấy danh sách đánh giá
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        List<Review> reviews = reviewRepository.findByProductId(productId);

        return reviews.stream().map(r -> {
            ReviewResponseDto dto = new ReviewResponseDto();
            dto.setId(r.getId());
            dto.setUserName(r.getUser().getName());
            dto.setRating(r.getRating());
            dto.setComment(r.getComment());
            dto.setCreatedAt(r.getCreatedAt());
            return dto;
        }).toList();
    }

}