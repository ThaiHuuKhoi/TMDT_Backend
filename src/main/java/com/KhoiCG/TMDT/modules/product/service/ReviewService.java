package com.KhoiCG.TMDT.modules.product.service;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.product.dto.ReviewResponseDto;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.event.ReviewCreatedEvent;
import com.KhoiCG.TMDT.modules.order.entity.OrderStatus;
import com.KhoiCG.TMDT.modules.order.repository.OrderRepository;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
import com.KhoiCG.TMDT.modules.user.entity.User;
import com.KhoiCG.TMDT.modules.user.repository.UserRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Caching;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private static final List<OrderStatus> PURCHASED_STATUSES = List.copyOf(
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.COMPLETED)
    );

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UserRepo userRepo;
    private final ApplicationEventPublisher eventPublisher;

    @Caching(evict = {
            @CacheEvict(value = "product_reviews", key = "#productId"),
            @CacheEvict(value = "product_details", key = "#productId"),
            @CacheEvict(value = "related_products", allEntries = true)
    })
    @Transactional
    public ReviewResponseDto createReview(Long userId, Long productId, Integer rating, String comment) {

        if (reviewRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "Bạn đã đánh giá sản phẩm này rồi!");
        }
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Người dùng không tồn tại"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Sản phẩm không tồn tại"));

        if (!orderRepository.existsPurchasedProduct(userId, productId, PURCHASED_STATUSES)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "PURCHASE_REQUIRED",
                    "Chỉ khách đã mua và thanh toán sản phẩm mới được đánh giá."
            );
        }

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(rating)
                .comment(comment)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("🧹 [CACHE EVICT] - Đã xóa cache Review của sản phẩm ID: {} vì có đánh giá mới", productId);

        eventPublisher.publishEvent(new ReviewCreatedEvent(productId));

        return toReviewResponseDto(savedReview);
    }

    private static ReviewResponseDto toReviewResponseDto(Review r) {
        ReviewResponseDto dto = new ReviewResponseDto();
        dto.setId(r.getId());
        dto.setUserName(r.getUser().getName());
        dto.setRating(r.getRating());
        dto.setComment(r.getComment());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setVerifiedPurchase(true);
        return dto;
    }

    @Cacheable(value = "product_reviews", key = "#productId")
    public List<ReviewResponseDto> getReviewsByProduct(Long productId) {
        log.info("[CACHE MISS] reviews productId={}", productId);
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(ReviewService::toReviewResponseDto)
                .toList();
    }
}