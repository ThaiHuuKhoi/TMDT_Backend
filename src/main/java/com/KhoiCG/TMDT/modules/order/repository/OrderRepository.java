package com.KhoiCG.TMDT.modules.order.repository;

import com.KhoiCG.TMDT.modules.order.entity.Order;
import com.KhoiCG.TMDT.modules.order.entity.OrderStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"user", "coupon"})
    List<Order> findByUserId(Long userId);

    @EntityGraph(attributePaths = {"user", "coupon", "items", "items.variant", "items.variant.product", "items.variant.product.images"})
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    boolean existsByStripeSessionId(String stripeSessionId);

    @Query("SELECT FUNCTION('MONTHNAME', o.createdAt), COUNT(o), " +
            "SUM(CASE WHEN o.status = 'COMPLETED' THEN 1 ELSE 0 END) " +
            "FROM Order o WHERE o.createdAt >= :startDate " +
            "GROUP BY FUNCTION('MONTH', o.createdAt), FUNCTION('MONTHNAME', o.createdAt) " +
            "ORDER BY FUNCTION('MONTH', o.createdAt) ASC")
    List<Object[]> getRawMonthlyStats(@Param("startDate") LocalDateTime startDate);

    Optional<Order> findByStripeSessionId(String stripeSessionId);

    Optional<Order> findByTrackingCode(String trackingCode);

    @EntityGraph(attributePaths = {"user", "coupon", "items", "items.variant", "items.variant.product", "items.variant.product.images"})
    @Query("SELECT o FROM Order o WHERE o.id = :id")
    Optional<Order> findDetailForAdmin(@Param("id") Long id);

    @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
    List<Object[]> countOrdersGroupedByStatus();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :st")
    BigDecimal sumTotalAmountByStatusIn(@Param("st") List<OrderStatus> statuses);

    @Query("SELECT FUNCTION('MONTHNAME', o.createdAt), COALESCE(SUM(o.totalAmount), 0) " +
            "FROM Order o WHERE o.createdAt >= :startDate AND o.status IN :st " +
            "GROUP BY FUNCTION('MONTH', o.createdAt), FUNCTION('MONTHNAME', o.createdAt) " +
            "ORDER BY FUNCTION('MONTH', o.createdAt) ASC")
    List<Object[]> getMonthlyRevenueStats(@Param("startDate") LocalDateTime startDate, @Param("st") List<OrderStatus> statuses);

    @Query("SELECT oi.productId, oi.productName, COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi JOIN oi.order o WHERE o.status IN :st " +
            "GROUP BY oi.productId, oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("st") List<OrderStatus> statuses, Pageable pageable);

    @Query("SELECT oi.productId, oi.productName, p.category.slug, p.category.name, " +
            "COALESCE(SUM(oi.quantity), 0), COALESCE(SUM(oi.priceAtPurchase * oi.quantity), 0) " +
            "FROM OrderItem oi " +
            "JOIN oi.order o " +
            "JOIN oi.variant v " +
            "JOIN v.product p " +
            "WHERE o.status IN :st AND o.createdAt >= :startDate " +
            "GROUP BY oi.productId, oi.productName, p.category.slug, p.category.name")
    List<Object[]> findProductSalesSince(
            @Param("st") List<OrderStatus> statuses,
            @Param("startDate") LocalDateTime startDate
    );

    @Query("""
            SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END
            FROM OrderItem oi
            JOIN oi.order o
            WHERE o.user.id = :userId
              AND oi.productId = :productId
              AND o.status IN :statuses
            """)
    boolean existsPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId,
            @Param("statuses") List<OrderStatus> statuses
    );
}