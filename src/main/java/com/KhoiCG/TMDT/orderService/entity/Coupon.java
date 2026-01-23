package com.KhoiCG.TMDT.orderService.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "coupons")
public class Coupon {
    @Id
    private String id;

    @Indexed(unique = true)
    private String code;

    private String discountType; // "PERCENT" hoặc "FIXED"

    // ⚠️ QUAN TRỌNG: Dùng Long cho tiền/phần trăm
    // Nếu PERCENT: 10 = 10%
    // Nếu FIXED: 50000 = 50.000 VNĐ
    private Long discountValue;

    private Long minOrderValue; // Ví dụ: 200000

    private Integer maxUsage;
    private Integer usedCount = 0;

    private LocalDateTime expiryDate;
    private boolean isActive = true;
}