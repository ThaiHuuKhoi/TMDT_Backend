package com.KhoiCG.TMDT.modules.order.repository;

import com.KhoiCG.TMDT.modules.order.entity.Coupon;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CouponRepository extends MongoRepository<Coupon, String> {
    // Tìm mã code (không phân biệt hoa thường nếu muốn, nhưng ở đây ta tìm chính xác)
    Optional<Coupon> findByCode(String code);
}