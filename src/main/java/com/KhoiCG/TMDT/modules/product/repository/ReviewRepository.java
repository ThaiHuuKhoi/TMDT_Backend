package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<Review> findByProductId(Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}