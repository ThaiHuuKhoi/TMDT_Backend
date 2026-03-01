package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.Review;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface ReviewRepository extends MongoRepository<Review, String> {
    // Lấy danh sách đánh giá của 1 sản phẩm, mới nhất lên đầu
    List<Review> findByProductIdOrderByCreatedAtDesc(String productId);

    // Lấy tất cả đánh giá của 1 sản phẩm (để tính trung bình)
    List<Review> findByProductId(String productId);
}