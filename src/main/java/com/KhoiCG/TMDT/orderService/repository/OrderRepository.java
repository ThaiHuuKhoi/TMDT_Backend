package com.KhoiCG.TMDT.orderService.repository;

import com.KhoiCG.TMDT.orderService.entity.Order;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {
    // Tìm order theo userId (cho user thường)
    List<Order> findByUserId(String userId);
    boolean existsByStripeSessionId(String stripeSessionId);
    // Tìm tất cả (cho admin), hỗ trợ phân trang/limit
    // Spring Data tự động hiểu Pageable để xử lý limit/sort
    // Nhưng method find logic đơn giản có thể dùng findAll(Pageable) có sẵn
}