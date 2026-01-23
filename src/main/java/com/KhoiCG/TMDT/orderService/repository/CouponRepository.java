package com.KhoiCG.TMDT.orderService.repository;

import com.KhoiCG.TMDT.orderService.entity.Coupon;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface CouponRepository extends MongoRepository<Coupon, String> {
    // Tìm mã code (không phân biệt hoa thường nếu muốn, nhưng ở đây ta tìm chính xác)
    Optional<Coupon> findByCode(String code);
}