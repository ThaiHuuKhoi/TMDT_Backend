package com.KhoiCG.TMDT.modules.product.listener;

import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.entity.Review;
import com.KhoiCG.TMDT.modules.product.event.ReviewCreatedEvent;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductRatingListener {

    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;

    @EventListener
    @Transactional
    public void handleReviewCreatedEvent(ReviewCreatedEvent event) {
        Long productId = event.getProductId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        List<Review> reviews = reviewRepository.findByProductId(productId);

        if (reviews.isEmpty()) {
            product.setAverageRating(0.0);
            product.setReviewCount(0);
        } else {
            double average = reviews.stream()
                    .mapToInt(Review::getRating)
                    .average()
                    .orElse(0.0);
            double roundedAverage = Math.round(average * 10.0) / 10.0;

            product.setAverageRating(roundedAverage);
            product.setReviewCount(reviews.size());
        }

        productRepository.save(product);
    }
}