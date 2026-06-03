package com.KhoiCG.TMDT.modules.product.repository;

import com.KhoiCG.TMDT.modules.product.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByProductIdOrderByCreatedAtDesc(Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.product.id = :productId")
    List<Object[]> findRatingStatsByProductId(Long productId);
}