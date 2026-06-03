package com.KhoiCG.TMDT.modules.product.listener;

import com.KhoiCG.TMDT.common.exception.ApiException;
import com.KhoiCG.TMDT.modules.product.entity.Product;
import com.KhoiCG.TMDT.modules.product.event.ReviewCreatedEvent;
import com.KhoiCG.TMDT.modules.product.repository.ProductRepository;
import com.KhoiCG.TMDT.modules.product.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm."));

        List<Object[]> statsList = reviewRepository.findRatingStatsByProductId(productId);
        Object[] stats = (statsList != null && !statsList.isEmpty()) ? statsList.get(0) : new Object[]{null, 0L};
        Double avg = (Double) stats[0];
        Long count = (Long) stats[1];

        product.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
        product.setReviewCount(count != null ? count.intValue() : 0);
        productRepository.save(product);
    }
}