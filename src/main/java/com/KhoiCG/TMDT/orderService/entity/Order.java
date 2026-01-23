package com.KhoiCG.TMDT.orderService.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "orders")
public class Order {
    @Id
    private String id;

    private String userId;
    private String email;
    private Long amount; // Lưu dưới dạng cents (số nguyên) giống code cũ
    private String status; // "success", "pending", etc.

    // Các trường sản phẩm (nếu có trong OrderType gốc, ở đây mình tạm ẩn để gọn)
    private String stripeSessionId;

    @CreatedDate
    private LocalDateTime createdAt;
}
