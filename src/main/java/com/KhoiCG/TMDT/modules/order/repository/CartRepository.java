package com.KhoiCG.TMDT.modules.order.repository;

import com.KhoiCG.TMDT.modules.order.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);
}