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

    @Indexed(unique = true) // Đảm bảo mã không trùng nhau
    private String code;    // Ví dụ: SALE50, TET2026

    private String discountType; // "PERCENT" (giảm %) hoặc "FIXED" (giảm tiền mặt)
    private Double discountValue; // Giá trị (10 nếu là 10%, 50000 nếu là 50k)

    private Double minOrderValue; // Đơn tối thiểu để áp dụng (VD: 200k)

    private Integer maxUsage;  // Tổng số lượt dùng tối đa
    private Integer usedCount = 0; // Số lượt đã dùng (Mặc định là 0)

    private LocalDateTime expiryDate; // Ngày hết hạn
    private boolean isActive = true;  // Trạng thái bật/tắt
}