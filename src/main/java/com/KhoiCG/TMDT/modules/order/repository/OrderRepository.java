package com.KhoiCG.TMDT.modules.order.repository;

import com.KhoiCG.TMDT.modules.order.dto.OrderChartResponse;
import com.KhoiCG.TMDT.modules.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> { // Đổi String thành Long

    List<Order> findByUserId(Long userId);

    boolean existsByStripeSessionId(String stripeSessionId);

    @Query("SELECT FUNCTION('MONTHNAME', o.createdAt), COUNT(o), " +
            "SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
            "FROM Order o WHERE o.createdAt >= :startDate " +
            "GROUP BY FUNCTION('MONTH', o.createdAt), FUNCTION('MONTHNAME', o.createdAt) " +
            "ORDER BY FUNCTION('MONTH', o.createdAt) ASC")
    List<Object[]> getRawMonthlyStats(@Param("startDate") LocalDateTime startDate);

    Optional<Order> findByIdAndUserId(Long id, Long userId); // Đổi String thành Long
    Optional<Order> findByStripeSessionId(String stripeSessionId);
}