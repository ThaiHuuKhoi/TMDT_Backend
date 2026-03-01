package com.KhoiCG.TMDT.modules.order.entity;

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

    private String discountType;

    private Long discountValue;

    private Long minOrderValue;

    private Integer maxUsage;
    private Integer usedCount = 0;

    private LocalDateTime expiryDate;
    private boolean isActive = true;
}