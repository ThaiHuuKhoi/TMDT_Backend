package com.KhoiCG.TMDT.modules.order.entity;

import com.KhoiCG.TMDT.modules.coupon.entity.Coupon;
import com.KhoiCG.TMDT.modules.shipping.entity.ShippingLog;
import com.KhoiCG.TMDT.modules.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(length = 10)
    @Builder.Default
    private String currency = "VND";

    @Column(name = "shipping_name")
    private String shippingName;

    @Column(name = "shipping_phone", length = 50)
    private String shippingPhone;

    @Column(columnDefinition = "TEXT")
    private String shippingAddress;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @BatchSize(size = 50)
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderStatusHistory> statusHistories = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "shipping_provider", length = 50)
    private String shippingProvider;

    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "expected_delivery_date")
    private LocalDateTime expectedDeliveryDate;

    // Mối quan hệ 1-Nhiều với bảng ShippingLogs
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("reportedAt DESC") // Sắp xếp giảm dần để Frontend dễ lấy log mới nhất
    @Builder.Default
    private List<ShippingLog> shippingLogs = new ArrayList<>();

    /** true khi kho đã bị trừ lúc tạo pending (VNPay). false khi chưa trừ (admin tạo tay). */
    @Column(name = "inventory_reserved")
    @Builder.Default
    private boolean inventoryReserved = false;

    public void addOrderItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public void addStatusHistory(OrderStatusHistory history) {
        statusHistories.add(history);
        history.setOrder(this);
    }
}